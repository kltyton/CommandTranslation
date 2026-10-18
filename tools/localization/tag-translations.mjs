import { readFile, readdir } from 'node:fs/promises';
import { resolve } from 'node:path';

export async function tagTranslations(root) {
  const directory = resolve(root, 'tools/localization/sources/tags');
  const load = async file => JSON.parse(await readFile(resolve(directory, file), 'utf8'));
  const catalog = await load('catalog.json');
  const loaderCatalog = await load('loader-catalog.json');
  const aliases = await load('aliases.json');
  const entries = {};
  const sources = {};
  async function collect(folder) {
    for (const file of await readdir(resolve(directory, folder), { withFileTypes: true })) {
      const path = folder + '/' + file.name;
      if (file.isDirectory()) { await collect(path); continue; }
      if (!file.name.endsWith('.json')) continue;
      const page = await load(path);
      Object.assign(sources, page.sources);
      for (const [locale, texts] of Object.entries(page)) {
        if (locale !== 'sources') Object.assign(entries[locale] ??= {}, texts);
      }
    }
  }
  await collect('loader');
  await collect('vanilla');
  const overrides = await load('overrides.json');
  Object.assign(sources, overrides.sources);
  for (const [locale, texts] of Object.entries(overrides)) {
    if (locale !== 'sources') Object.assign(entries[locale] ??= {}, texts);
  }
  for (const locale of Object.keys(entries)) {
    for (const [key, target] of Object.entries(aliases.translations)) {
      const text = entries[locale][target] ?? entries.en_us[target];
      if (text) entries[locale][key] = text;
    }
    Object.assign(entries[locale], aliases.native);
  }
  for (const collection of [catalog, loaderCatalog]) {
    for (const [registry, tags] of Object.entries(collection.registries)) {
      for (const id of Object.keys(tags)) {
        const key = 'command_translation.tag.' + registry.replaceAll('/', '.') + '.' + id.replace(':', '.').replaceAll('/', '.');
        for (const locale of ['en_us', 'zh_cn']) {
          if (!entries[locale]?.[key]) throw new Error('Missing tag translation: ' + locale + ' ' + registry + ' ' + id);
        }
      }
    }
  }
  return { entries, sources, aliases, catalog, loaderCatalog };
}
