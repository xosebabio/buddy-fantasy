// Writes src/locale/messages.xlf with `ng extract-i18n` and fails if an id is repeated or does not
// follow `@@<layer or feature>.<screen>.<text>` (decision 7 of plan 11, RNF-15). The extractor only
// warns about a repeated id, and keeps one of its texts. Run it with `npm run i18n:extract`.
import { spawnSync } from 'node:child_process';
import { readdirSync, readFileSync } from 'node:fs';

const MESSAGES = new URL('../src/locale/messages.xlf', import.meta.url);
const FEATURES = new URL('../src/app/features/', import.meta.url);

const extraction = spawnSync('npx', ['ng', 'extract-i18n'], { encoding: 'utf8' });
process.stdout.write(extraction.stdout);
process.stderr.write(extraction.stderr);
if (extraction.status !== 0) {
  process.exit(extraction.status ?? 1);
}

const problems = [];
if (/Duplicate messages with id/.test(extraction.stdout + extraction.stderr)) {
  problems.push('An id is used for more than one text (see the warnings above).');
}

const layers = ['core', 'ui', 'shared', ...readdirSync(FEATURES)];
const pattern = new RegExp(`^(${layers.join('|')})(\\.[a-z][A-Za-z0-9]*)+$`);
const ids = [...readFileSync(MESSAGES, 'utf8').matchAll(/<trans-unit id="([^"]*)"/g)].map(
  ([, id]) => id,
);
for (const id of ids.filter((id) => !pattern.test(id))) {
  problems.push(`"${id}" does not follow @@<${layers.join('|')}>.<screen>.<text>.`);
}

if (problems.length > 0) {
  console.error(`\ni18n:extract failed:\n${problems.map((problem) => ` - ${problem}`).join('\n')}`);
  process.exit(1);
}
