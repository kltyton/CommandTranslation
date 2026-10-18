import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const revision = '6bda4a11d30005697e82ea9043e7d486652542b9';
const upstream = 'https://raw.githubusercontent.com/Yeah-Zero/Identifier-Translation/' + revision + '/';
const destination = resolve(root, 'tools/localization/sources/identifiers');
const cache = process.argv[2] ? resolve(process.argv[2], revision) : null;
const locales = ['cs_cz', 'de_de', 'en_us', 'es_es', 'fr_fr', 'hu_hu', 'it_it', 'ja_jp', 'ko_kr', 'lzh', 'nl_nl', 'pl_pl', 'pt_pt', 'ru_ru', 'th_th', 'tr_tr', 'tt_ru', 'uk_ua', 'zh_cn', 'zh_hk', 'zh_tw'];
await mkdir(destination, { recursive: true });
if (cache) await mkdir(cache, { recursive: true });
await mkdir(resolve(root, 'licenses'), { recursive: true });

async function download(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(45000) });
  if (!response.ok) throw new Error(response.status + ': ' + url);
  return response.text();
}

for (const locale of locales) {
  const file = '1.20-1.20.1/src/main/resources/assets/identifier_translation/lang/' + locale + '.json';
  let text;
  if (cache) {
    try { text = await readFile(resolve(cache, locale + '.json'), 'utf8'); }
    catch (failure) { if (failure.code !== 'ENOENT') throw failure; }
  }
  if (text === undefined) {
    text = await download(upstream + file);
    if (cache) await writeFile(resolve(cache, locale + '.json'), text, 'utf8');
  }
  const entries = JSON.parse(text);
  await writeFile(resolve(destination, locale + '.json'), JSON.stringify(entries, null, 2) + '\n', 'utf8');
  console.log(locale + ': ' + Object.keys(entries).length + ' identifier translations');
}
await writeFile(resolve(destination, 'source.json'), JSON.stringify({
  project: 'Identifier Translation', author: 'Yeah-Zero', revision,
  url: 'https://github.com/Yeah-Zero/Identifier-Translation/tree/' + revision,
  license: 'LGPL-3.0-only', locales
}, null, 2) + '\n', 'utf8');
await writeFile(resolve(root, 'licenses/identifier-translation-LGPL-3.0.txt'), await download(upstream + 'LICENSE'), 'utf8');
await writeFile(resolve(root, 'licenses/GPL-3.0.txt'),
  await download('https://raw.githubusercontent.com/spdx/license-list-data/main/text/GPL-3.0-only.txt'), 'utf8');
try { await readFile(resolve(root, 'licenses/template-MIT.txt'), 'utf8'); }
catch (failure) {
  if (failure.code !== 'ENOENT') throw failure;
  await writeFile(resolve(root, 'licenses/template-MIT.txt'), await readFile(resolve(root, 'LICENSE'), 'utf8'), 'utf8');
}
await writeFile(resolve(root, 'LICENSE'),
  await download('https://raw.githubusercontent.com/spdx/license-list-data/main/text/LGPL-3.0-only.txt'), 'utf8');
