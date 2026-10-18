import { createHash } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

export class WikiHttpError extends Error {
  constructor(status, url) { super(`${status}: ${url}`); this.code = 'HTTP_' + status; }
}

export async function readWiki(cache, host, parameters) {
  const url = new URL('/api.php', host);
  url.search = new URLSearchParams({ format: 'json', formatversion: '2', ...parameters }).toString();
  const file = resolve(cache, createHash('sha256').update(url.href).digest('hex') + '.json');
  let payload;
  try { payload = await readFile(file, 'utf8'); }
  catch (failure) {
    if (failure.code !== 'ENOENT') throw failure;
    const response = await fetch(url, { signal: AbortSignal.timeout(45000),
      headers: { 'User-Agent': 'CommandTranslation-localization/0.1 (Minecraft command documentation)' } });
    if (!response.ok) throw new WikiHttpError(response.status, url.href);
    payload = await response.text();
    const parsed = JSON.parse(payload);
    if (parsed.error) throw new Error(JSON.stringify(parsed.error));
    await writeFile(file, payload, 'utf8');
  }
  return JSON.parse(payload);
}
