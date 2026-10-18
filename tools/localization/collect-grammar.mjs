import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const cache = process.argv[2];
if (!cache) throw new Error('Usage: node tools/localization/collect-grammar.mjs <source-cache-directory>');
const versions = ['1.20.1', '1.21.1', '26.1.2', '26.2'];
const repository = 'https://github.com/misode/mcmeta';
const sources = [];
let references;

function revisionFor(tag) {
  references ??= new Map(execFileSync('git', ['-c', 'credential.helper=', 'ls-remote', repository + '.git',
    ...versions.flatMap(version => ['refs/tags/' + version + '-summary', 'refs/tags/' + version + '-summary^{}'])],
  { encoding: 'utf8', windowsHide: true, env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
    .trim().split(/\r?\n/).filter(Boolean).map(line => {
      const [revision, ref] = line.split('\t');
      return [ref, revision];
    }));
  const revision = references.get('refs/tags/' + tag + '^{}') ?? references.get('refs/tags/' + tag);
  if (!revision) throw new Error('Missing command report tag: ' + tag);
  return revision;
}

async function request(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(45000),
    headers: { 'User-Agent': 'CommandTranslation-localization/0.1' } });
  if (!response.ok) throw new Error(`${response.status}: ${url}`);
  return response.json();
}

for (const version of versions) {
  const directory = resolve(cache, version);
  await mkdir(directory, { recursive: true });
  const metadataFile = resolve(directory, 'source.json');
  let metadata;
  try { metadata = JSON.parse(await readFile(metadataFile, 'utf8')); }
  catch (failure) { if (failure.code !== 'ENOENT') throw failure; }
  if (!metadata) {
    const tag = version + '-summary';
    const revision = revisionFor(tag);
    metadata = { version, repository, tag, revision,
      url: 'https://raw.githubusercontent.com/misode/mcmeta/' + revision + '/commands/data.json' };
    await writeFile(metadataFile, JSON.stringify(metadata, null, 2) + '\n', 'utf8');
  }
  const treeFile = resolve(directory, 'commands.json');
  let tree;
  try { tree = JSON.parse(await readFile(treeFile, 'utf8')); }
  catch (failure) { if (failure.code !== 'ENOENT') throw failure; }
  if (!tree) {
    tree = await request(metadata.url);
    if (tree.type !== 'root' || !tree.children) throw new Error('Expected a Brigadier root in ' + metadata.url);
    await writeFile(treeFile, JSON.stringify(tree, null, 2) + '\n', 'utf8');
  }
  sources.push(metadata);
  console.log(version + ': ' + Object.keys(tree.children).length + ' command roots');
}

await writeFile(resolve(root, 'tools/localization/sources/grammar.json'), JSON.stringify({
  description: 'Version-pinned copies of vanilla data-generator command reports, used to maintain documentation coverage.',
  sources
}, null, 2) + '\n', 'utf8');
