import { mkdir, writeFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { readWiki } from './wiki/api.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const cache = process.argv[2];
if (!cache) throw new Error('Usage: node tools/localization/collect-references.mjs <source-cache-directory>');
await mkdir(cache, { recursive: true });
const host = 'https://minecraft.wiki';
const titles = ['Dialog', 'Game_rule', 'Target_selectors', 'Argument_types', 'Locator_Bar', 'Timeline', 'World_clock',
  'Difficulty', 'Game_mode', 'Java_Edition_1.21.5', 'Slot', 'Heightmap', 'Data_component_format',
  'Block_states', 'Raw_JSON_text_format', 'Entity_format', 'Chunk_format', 'Player.dat_format', 'Particle',
  'Template:Nbt_inherit/potion_contents/template', 'Template:Nbt_inherit/consume_effect/template',
  'Block_entity_format', 'Item_format/Before_1.20.5', 'Particle_format'];
const pages = await readWiki(cache, host, { action: 'query', redirects: '1', prop: 'revisions',
  rvprop: 'ids|timestamp', titles: titles.join('|') });
const sources = [];
for (const page of pages.query.pages) {
  if (page.missing) throw new Error('Missing Wiki reference: ' + page.title);
  const content = await readWiki(cache, host, { action: 'parse', page: page.title, prop: 'text', disablelimitreport: '1' });
  sources.push({ locale: 'en_us', title: page.title, revision: page.revisions[0].revid,
    url: host + '/w/' + encodeURIComponent(page.title) });
  console.log(page.title + ': revision ' + page.revisions[0].revid + ', ' + content.parse.text.length + ' HTML characters');
}
await writeFile(resolve(root, 'tools/localization/sources/references.json'), JSON.stringify({ sources }, null, 2) + '\n', 'utf8');
