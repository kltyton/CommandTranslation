import { readFile, writeFile, mkdir, readdir, copyFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
if (process.argv[2] === '--references' || process.argv[2] === '--tags') {
  const tags = process.argv[2] === '--tags';
  const [cache, destination, python] = process.argv.slice(3);
  if (!cache || !destination || !python) throw new Error('Usage: node tools/localization/prepare-help-input.mjs --references <wiki-cache> <input-directory> <python>');
  const references = JSON.parse(await readFile(resolve(root, 'tools/localization/sources/' + (tags ? 'tags/catalog.json' : 'references.json')), 'utf8'));
  const wanted = new Set(tags ? references.sources.map(source => source.title) : ['Target selectors', 'Block states', 'Text component format', 'Entity format', 'Chunk format', 'Player.dat format', 'Particles', 'Dialog', 'Argument types',
    'Template:Nbt inherit/potion contents/template', 'Template:Nbt inherit/consume effect/template',
    'Block entity format', 'Item format/Before 1.20.5', 'Particle format']);
  const pages = new Map();
  for (const file of await readdir(cache)) {
    const document = JSON.parse(await readFile(resolve(cache, file), 'utf8'));
    if (wanted.has(document.parse?.title) && typeof document.parse.text === 'string') pages.set(document.parse.title, document.parse);
  }
  const folder = resolve(destination, tags ? 'tags/references' : 'references');
  await mkdir(folder, { recursive: true });
  let count = 0;
  for (const source of references.sources.filter(source => wanted.has(source.title))) {
    const page = pages.get(source.title);
    if (!page) throw new Error('Missing cached reference: ' + source.title);
    const result = spawnSync(python, [resolve(root, 'tools/localization/parse-wiki.py'), '--text'], {
      input: JSON.stringify([{ locale: 'en_us', commands: [], html: page.text }]), encoding: 'utf8', windowsHide: true,
      maxBuffer: 8 * 1024 * 1024
    });
    if (result.status !== 0) throw new Error(result.stderr || result.error?.message);
    const text = JSON.parse(result.stdout)[0].text;
    await writeFile(resolve(folder, source.title.replaceAll(' ', '_').replaceAll('/', '_').replaceAll(':', '_') + '.json'), JSON.stringify({ ...source, text }, null, 2) + '\n', 'utf8');
    count++;
  }
  if (count !== wanted.size) throw new Error('Reference metadata is incomplete');
  if (tags) {
    const catalog = resolve(destination, 'tags/catalog.json');
    await writeFile(catalog, JSON.stringify(references, null, 2) + '\n', 'utf8');
  }
  console.log(count + ' public reference inputs prepared');
  process.exit(0);
}
if (process.argv[2] === '--descriptions') {
  const destination = process.argv[3];
  if (!destination) throw new Error('Usage: node tools/localization/prepare-help-input.mjs --descriptions <input-directory>');
  const directory = resolve(root, 'tools/localization/sources/parameters');
  const output = resolve(destination, 'descriptions');
  await mkdir(output, { recursive: true });
  let count = 0;
  for (const file of (await readdir(directory)).filter(name => name.endsWith('.json'))) {
    const page = JSON.parse(await readFile(resolve(directory, file), 'utf8'));
    if (!page.syntax) continue;
    const descriptions = Object.fromEntries(['en_us', 'zh_cn', 'de_de', 'ja_jp', 'sources'].map(key => [key, page[key]]));
    await writeFile(resolve(output, file), JSON.stringify(descriptions, null, 2) + '\n', 'utf8');
    count++;
  }
  console.log(count + ' command description inputs prepared');
  process.exit(0);
}
const [grammarCache, wikiCache, destination] = process.argv.slice(2);
if (!grammarCache || !wikiCache || !destination) {
  throw new Error('Usage: node tools/localization/prepare-help-input.mjs <grammar-cache> <wiki-cache> <input-directory>');
}
const sourceDirectory = resolve(root, 'tools/localization/sources');
const load = async file => JSON.parse(await readFile(file, 'utf8'));
const grammar = await load(resolve(sourceDirectory, 'grammar.json'));
const wiki = await load(resolve(sourceDirectory, 'wiki.json'));
const references = await load(resolve(sourceDirectory, 'references.json'));
const commands = new Set();
const nodesByRoot = new Map();
const grammarOutput = resolve(destination, 'command-grammar');
await mkdir(grammarOutput, { recursive: true });
for (const source of grammar.sources) {
  const file = resolve(grammarCache, source.version, 'commands.json');
  const tree = await load(file);
  for (const name of Object.keys(tree.children)) commands.add(name);
  const visit = (node, path) => {
    for (const [name, child] of Object.entries(node.children ?? {})) {
      const names = [...path, name];
      const entries = nodesByRoot.get(names[0]) ?? new Map();
      nodesByRoot.set(names[0], entries);
      const key = JSON.stringify([names, child.type, child.parser, child.properties]);
      const entry = entries.get(key) ?? { path: names.join('.'), type: child.type,
        parser: child.parser, properties: child.properties, versions: [] };
      entry.versions.push(source.version);
      entries.set(key, entry);
      visit(child, names);
    }
  };
  visit(tree, []);
  await copyFile(file, resolve(grammarOutput, source.version + '.json'));
}
const nodeDirectory = resolve(destination, 'nodes');
await mkdir(nodeDirectory, { recursive: true });
for (const [command, entries] of nodesByRoot) {
  await writeFile(resolve(nodeDirectory, command + '.json'), JSON.stringify({ command, nodes: [...entries.values()] }, null, 2) + '\n', 'utf8');
}
const pages = new Map([...wiki.sources.filter(source => source.locale === 'en_us'
  && source.commands.some(command => commands.has(command))), ...references.sources]
  .map(source => [source.title, source]));
const found = new Set();
for (const file of await readdir(wikiCache)) {
  if (!file.endsWith('.json')) continue;
  const document = await load(resolve(wikiCache, file));
  const page = document.parse;
  if (!page || !pages.has(page.title) || typeof page.text !== 'string') continue;
  const source = pages.get(page.title);
  const folder = resolve(destination, 'wiki', page.title.startsWith('Commands/') ? 'commands' : 'references');
  await mkdir(folder, { recursive: true });
  const title = page.title.startsWith('Commands/') ? page.title.slice('Commands/'.length) : page.title;
  await writeFile(resolve(folder, encodeURIComponent(title) + '.json'), JSON.stringify({
    title: page.title, url: source.url, revision: page.revid ?? source.revision, html: page.text
  }, null, 2) + '\n', 'utf8');
  found.add(page.title);
}
const missing = [...pages.keys()].filter(title => !found.has(title));
if (missing.length) throw new Error('Missing cached Wiki pages: ' + missing.join(', '));
const translations = resolve(destination, 'translations');
await mkdir(translations, { recursive: true });
await writeFile(resolve(translations, 'en_us.json'), JSON.stringify(wiki.entries.en_us, null, 2) + '\n', 'utf8');
await copyFile(resolve(sourceDirectory, 'zh_cn.json'), resolve(translations, 'zh_cn.json'));
const vocabulary = resolve(destination, 'vocabulary');
await mkdir(vocabulary, { recursive: true });
for (const name of ['finite', 'vanilla', 'commands']) {
  await copyFile(resolve(root, 'tools/localization/vocabulary', name + '.json'), resolve(vocabulary, name + '.json'));
}
await writeFile(resolve(destination, 'sources.json'), JSON.stringify({ grammar, wiki: [...pages.values()] }, null, 2) + '\n', 'utf8');
console.log(commands.size + ' native command roots, ' + found.size + ' English Wiki pages prepared');
