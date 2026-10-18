import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { readWiki, WikiHttpError } from './wiki/api.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const cache = process.argv[2];
if (!cache) throw new Error('Usage: node tools/localization/collect-wiki.mjs <source-cache-directory>');
await mkdir(cache, { recursive: true });
const vocabulary = JSON.parse(await readFile(resolve(root, 'tools/localization/vocabulary/commands.json'), 'utf8'));
const aliases = { tell: 'msg', w: 'msg', tp: 'teleport', experience: 'xp', tm: 'teammsg', 'ban-ip': 'ban', banlist: 'ban', 'pardon-ip': 'pardon', 'save-all': 'save', 'save-off': 'save', 'save-on': 'save' };
const commands = Object.keys(vocabulary.commands);
const pages = [...new Set(commands.map(command => 'Commands/' + (aliases[command] ?? command)))];

const api = (host, parameters) => readWiki(cache, host, parameters);

async function collect(host, titles) {
  const result = [];
  for (let index = 0; index < titles.length; index += 10) {
    const requested = titles.slice(index, index + 10);
    const data = await api(host, { action: 'query', redirects: '1', prop: 'extracts|langlinks|revisions', exintro: '1', explaintext: '1', exlimit: 'max', lllimit: 'max', rvprop: 'ids|timestamp', titles: requested.join('|') });
    const redirects = new Map([...(data.query.normalized ?? []), ...(data.query.redirects ?? [])].map(row => [row.from, row.to]));
    const resolved = title => {
      const seen = new Set();
      while (redirects.has(title)) {
        if (seen.has(title)) throw new Error('Cyclic Wiki redirect: ' + title);
        seen.add(title);
        title = redirects.get(title);
      }
      return title;
    };
    result.push(...data.query.pages.map(page => ({ ...page, requestedTitles: requested.filter(title => resolved(title) === page.title) })));
  }
  return result;
}

const english = await collect('https://minecraft.wiki', pages);
const site = await api('https://minecraft.wiki', { action: 'query', meta: 'siteinfo', siprop: 'interwikimap|rightsinfo' });
const languageSites = site.query.interwikimap.filter(site => site.language);
const documentation = { en_us: {} };
const sources = [];
const html = [];
const failures = [];
const skipped = [];
const pageCommands = page => commands.filter(command => page.requestedTitles.includes('Commands/' + (aliases[command] ?? command)));

async function storePage(locale, host, page, roots) {
  if (page.missing) return;
  const text = (page.extract ?? '').trim();
  if (text) for (const command of roots) documentation[locale][`command_translation.help.command.${command}`] = text.split('\n\n')[0];
  sources.push({ locale, title: page.title, revision: page.revisions?.[0]?.revid, url: host + '/w/' + encodeURIComponent(page.title), commands: roots });
  const rendered = await api(host, { action: 'parse', page: page.title, prop: 'text', disablelimitreport: '1' });
  html.push({ locale, commands: roots, html: rendered.parse.text });
}

for (const page of english) await storePage('en_us', 'https://minecraft.wiki', page, pageCommands(page));
const localeSites = { cs: ['cs_cz'], de: ['de_de'], el: ['el_gr'], es: ['es_es'], fr: ['fr_fr'], he: ['he_il'], hu: ['hu_hu'], id: ['id_id'], it: ['it_it'], ja: ['ja_jp'], ko: ['ko_kr'], lzh: ['lzh'], nl: ['nl_nl'], pl: ['pl_pl'], pt: ['pt_pt', 'pt_br'], 'pt-br': ['pt_br'], ru: ['ru_ru'], th: ['th_th'], tr: ['tr_tr'], uk: ['uk_ua'], zh: ['zh_cn', 'zh_tw', 'zh_hk'] };
for (const language of languageSites) {
  if (language.prefix === 'en') continue;
  if (process.env.WIKI_LANGUAGES && !process.env.WIKI_LANGUAGES.split(',').includes(language.prefix)) continue;
  const locales = localeSites[language.prefix];
  if (!locales) throw new Error('Wiki language requires a Minecraft locale mapping: ' + language.prefix);
  const linked = new Map();
  let missing = 0;
  for (const page of english) {
    if (page.missing) continue;
    const link = page.langlinks?.find(link => link.lang === language.prefix);
    if (link) linked.set(link.title, [...new Set([...(linked.get(link.title) ?? []), ...pageCommands(page)])]);
    else missing++;
  }
  if (language.prefix !== 'zh' && missing > 0) {
    skipped.push({ language: language.prefix, pages: linked.size, reason: 'incomplete_command_pages' });
    console.log(language.prefix + ': skipped incomplete command documentation');
    continue;
  }
  const host = new URL(language.url).origin;
  try {
    const localized = await collect(host, [...linked.keys()]);
    for (const locale of locales) {
      documentation[locale] ??= {};
      for (const page of localized) await storePage(locale, host, page,
        [...new Set(page.requestedTitles.flatMap(title => linked.get(title) ?? []))]);
    }
    console.log(language.prefix + ': ' + localized.length + ' command pages');
  } catch (failure) {
    const code = failure instanceof WikiHttpError ? failure.code
      : failure instanceof DOMException && failure.name === 'TimeoutError' ? 'ETIMEDOUT' : failure.cause?.code;
    if (!(failure instanceof WikiHttpError) && code !== 'ETIMEDOUT'
        && (!(failure instanceof TypeError) || !['ECONNRESET', 'ENOTFOUND', 'UND_ERR_CONNECT_TIMEOUT'].includes(code))) throw failure;
    failures.push({ language: language.prefix, host, code, message: failure.message });
    console.error(language.prefix + ': source unavailable: ' + code);
  }
}

const parsed = spawnSync(process.env.PYTHON ?? 'python', [resolve(root, 'tools/localization/parse-wiki.py')], { input: JSON.stringify(html), encoding: 'utf8', maxBuffer: 32 * 1024 * 1024, windowsHide: true, env: { ...process.env, PYTHONIOENCODING: 'utf-8', PYTHONDONTWRITEBYTECODE: '1' } });
if (parsed.status !== 0) throw new Error(parsed.stderr);
const extracted = JSON.parse(parsed.stdout);
for (const row of extracted) {
  for (const command of row.commands) for (const [name, text] of Object.entries(row.arguments)) {
    documentation[row.locale][`command_translation.help.argument.${command}.${name}`] = text;
  }
}
const destination = resolve(root, 'tools/localization/sources/wiki.json');
await mkdir(dirname(destination), { recursive: true });
await writeFile(destination, JSON.stringify({ rights: site.query.rightsinfo, languageSites, sources,
  argumentDefinitions: extracted.map(({ locale, commands, definitions }) => ({ locale, commands, definitions })),
  entries: documentation }, null, 2) + '\n', 'utf8');
await writeFile(resolve(cache, 'collection-status.json'), JSON.stringify({ failures, skipped }, null, 2) + '\n', 'utf8');
console.log('Collected ' + sources.length + ' command documentation sources');
