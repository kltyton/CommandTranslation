import { readFile, writeFile, mkdir, readdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import OpenCC from 'opencc-js';
import { tagTranslations } from './tag-translations.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const load = async path => JSON.parse(await readFile(resolve(root, 'tools/localization', path), 'utf8'));
const commands = await load('vocabulary/commands.json');
const vanilla = await load('vocabulary/vanilla.json');
const finite = await load('vocabulary/finite.json');
const configuration = await load('vocabulary/configuration.json');
const wiki = await load('sources/wiki.json');
const translatedHelp = {
  zh_cn: await load('sources/zh_cn.json'),
  de_de: await load('sources/de_de.json'),
  ja_jp: await load('sources/ja_jp.json')
};
const overrides = await load('sources/overrides.json');
const grammar = await load('sources/grammar.json');
const references = await load('sources/references.json');
const tags = await tagTranslations(root);
const parameterDirectory = resolve(root, 'tools/localization/sources/parameters');
const parameterHelp = {};
const parameterSources = {};
const componentCatalog = await load('sources/components.json');
async function parameterFiles(directory, prefix = '') {
  const files = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (entry.isDirectory()) files.push(...await parameterFiles(resolve(directory, entry.name), prefix + entry.name + '/'));
    else if (entry.name.endsWith('.json')) files.push(prefix + entry.name);
  }
  return files.sort();
}
for (const file of await parameterFiles(parameterDirectory)) {
  const page = await load('sources/parameters/' + file);
  for (const node of page.syntax ?? []) {
    if (node.path.includes('.') && !page.bindings?.[node.path]) {
      throw new Error('Undocumented command node: ' + file + ' ' + node.path);
    }
  }
  for (const key of Object.keys(page.en_us)) {
    if (!page.sources?.[key]?.length) throw new Error('Missing Wiki provenance: ' + file + ' ' + key);
    for (const locale of ['en_us', 'zh_cn', 'de_de', 'ja_jp']) {
      if (typeof page[locale]?.[key] !== 'string' || !page[locale][key].trim()) {
        throw new Error('Missing parameter translation: ' + file + ' ' + locale + ' ' + key);
      }
    }
  }
  Object.assign(parameterSources, page.sources);
  for (const [path, key] of Object.entries(page.bindings ?? {})) {
    if (!path.includes('.')) continue;
    if (!page.en_us?.[key] || !page.zh_cn?.[key]) throw new Error('Missing bilingual parameter text: ' + file + ' ' + path);
    const sources = page.sources?.[key];
    if (!sources?.length) throw new Error('Missing Wiki provenance: ' + file + ' ' + key);
    parameterSources['command_translation.help.argument.' + path] = sources;
  }
  for (const locale of ['en_us', 'zh_cn', 'de_de', 'ja_jp']) {
    const entries = parameterHelp[locale] ??= {};
    Object.assign(entries, page[locale]);
    for (const [path, key] of Object.entries(page.bindings ?? {})) {
      if (!path.includes('.')) continue;
      const text = page[locale]?.[key];
      if (text) entries['command_translation.help.argument.' + path] = text;
    }
  }
}
for (const id of Object.keys(componentCatalog.components)) {
  const key = 'command_translation.help.component.' + id;
  if (!parameterHelp.en_us?.[key] || !parameterHelp.zh_cn?.[key]) throw new Error('Missing component documentation: ' + id);
}
const identifierFiles = await readdir(resolve(root, 'tools/localization/sources/identifiers'));
const identifierLocales = identifierFiles.filter(name => name.endsWith('.json') && name !== 'source.json').map(name => name.slice(0, -5)).sort();
const output = resolve(root, 'common/src/generated/resources/assets/command_translation');
await mkdir(resolve(output, 'lang'), { recursive: true });
await mkdir(resolve(output, 'documentation'), { recursive: true });
const prefix = 'command_translation.';
const descriptionText = text => text.replace(/\r\n?/g, '\n').trim().replace(/%/g, '%%');
const requiredLocales = ['en_us', 'zh_cn', 'de_de', 'ja_jp'];
const locales = [...requiredLocales, ...identifierLocales.filter(locale => !requiredLocales.includes(locale) && !['zh_tw', 'zh_hk'].includes(locale))];
const helpKeys = new Set([...Object.keys(wiki.entries.en_us), ...Object.keys(overrides.en_us ?? {}),
  ...Object.keys(parameterHelp.en_us ?? {})].filter(key => key.startsWith(prefix + 'help.')
    && (['range', 'value', 'registry', 'component', 'selector', 'block_state', 'nbt', 'text', 'particle', 'dialog', 'coordinate', 'argument', 'syntax'].some(family => key.startsWith(prefix + 'help.' + family + '.'))
      || Object.hasOwn(commands.commands, key.split('.')[3]))));
const snapshots = new Map();

async function identifiers(locale) {
  const source = await load('sources/identifiers/' + locale + '.json');
  return Object.fromEntries(Object.entries(source).map(([key, value]) => [prefix + key, value]));
}

function commandNames(entries, locale, names) {
  const index = commands.locales.indexOf(locale);
  if (index < 0) throw new Error('Missing command locale: ' + locale);
  for (const [command, translations] of Object.entries(commands.commands)) {
    entries[prefix + 'command.' + command + '.alias'] = translations[index] ?? names[command] ?? translations[0];
  }
}

async function save(locale, entries) {
  await writeFile(resolve(output, 'lang', locale + '.json'),
    JSON.stringify(Object.fromEntries(Object.entries(entries).sort(([a], [b]) => a.localeCompare(b))), null, 2) + '\n', 'utf8');
  snapshots.set(locale, entries);
  console.log(locale + ': ' + Object.keys(entries).length + ' translation keys');
}

for (const locale of locales) {
  const entries = {};
  for (const [key, value] of Object.entries(configuration[locale] ?? {})) { entries[prefix + key] = value; }
  const names = locale === 'en_us'
    ? Object.fromEntries([...new Set([...Object.keys(vanilla.zh_cn), ...Object.keys(finite.zh_cn)])].map(token => [token, token]))
    : { ...vanilla[locale], ...finite[locale] };
  for (const [token, alias] of Object.entries(names)) {
    entries[prefix + 'literal.' + token + '.alias'] = alias;
    entries[prefix + 'value.' + token + '.alias'] = alias;
    entries[prefix + 'command.' + token + '.alias'] = alias;
  }
  commandNames(entries, locale, names);
  Object.assign(entries, await identifiers(locale));
  Object.assign(entries, tags.entries[locale]);
  Object.assign(entries, Object.fromEntries(Object.entries(parameterHelp[locale] ?? {}).filter(([key]) => key.startsWith(prefix + 'registry.') || key.startsWith(prefix + 'syntax.'))));
  const descriptions = { ...(translatedHelp[locale] ?? {}), ...wiki.entries[locale], ...overrides[locale],
    ...parameterHelp[locale] };
  for (const key of helpKeys) {
    const description = descriptions[key];
    if (typeof description !== 'string' || !description.trim()) {
      if (requiredLocales.includes(locale)) throw new Error('Missing documentation: ' + locale + ' ' + key);
      continue;
    }
    entries[key] = key.startsWith(prefix + 'help.range.') || key === prefix + 'help.value.context' ? description.trim() : descriptionText(description);
  }
  for (const [key, text] of Object.entries(overrides[locale] ?? {})) {
    if (!key.startsWith(prefix + 'help.')) { entries[key] = text; }
  }
  entries[prefix + 'identifier.variant'] = '%s (%s)';
  if (configuration[locale]?.['config.title']) { entries[prefix + 'name'] = configuration[locale]['config.title']; }
  await save(locale, entries);
}

for (const [locale, variant] of [['zh_tw', 'twp'], ['zh_hk', 'hk']]) {
  const convert = OpenCC.Converter({ from: 'cn', to: variant });
  const entries = Object.fromEntries(Object.entries(snapshots.get('zh_cn')).map(([key, value]) => [key, convert(value)]));
  Object.assign(entries, await identifiers(locale));
  commandNames(entries, locale, Object.fromEntries(Object.entries(vanilla.zh_cn).map(([key, value]) => [key, convert(value)])));
  await save(locale, entries);
}

await writeFile(resolve(output, 'documentation/sources.json'), JSON.stringify({
  license: wiki.rights, sources: wiki.sources.filter(source => source.commands.some(command => Object.hasOwn(commands.commands, command))),
  syntaxSources: grammar.sources,
  referenceSources: references.sources, parameterSources,
  tagSources: { wiki: tags.catalog.sources, native: tags.catalog.nativeSources,
    loaders: tags.loaderCatalog.artifacts, entries: tags.sources, aliases: tags.aliases }, translations: {
    zh_cn: { basedOn: 'en_us', description: 'Chinese translations and concise summaries of the linked English Minecraft Wiki revisions.' },
    de_de: { basedOn: ['de_de', 'en_us'], description: 'German Minecraft Wiki entries supplemented with translations of the linked English revisions.' },
    ja_jp: { basedOn: ['ja_jp', 'en_us'], description: 'Japanese Minecraft Wiki entries supplemented with translations of the linked English revisions.' },
    zh_tw: { basedOn: 'zh_cn', converter: 'opencc-js 1.4.2 cn/twp', description: 'Traditional Chinese conversion with upstream regional identifier names.' },
    zh_hk: { basedOn: 'zh_cn', converter: 'opencc-js 1.4.2 cn/hk', description: 'Hong Kong Traditional Chinese conversion with upstream regional identifier names.' }
  }
}, null, 2) + '\n', 'utf8');
