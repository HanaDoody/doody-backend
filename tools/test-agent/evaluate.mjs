import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { runGradle, SERVICE, TEST, TEST_CLASS, sha, parseJUnit, classifyRun } from './core.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const label = process.argv[2] || `evaluation-${Date.now()}`;
if (!/^[a-zA-Z0-9_-]{1,64}$/.test(label)) throw new Error('Invalid evaluation name');
const output = path.join(root, '.agent-runs', label);
const refresh = process.argv.includes('--refresh');
if (!refresh) await fs.mkdir(output, { recursive: false });
const previous = refresh ? JSON.parse(await fs.readFile(path.join(output, 'evaluation.json'), 'utf8')) : null;
const original = await fs.readFile(path.join(root, SERVICE), 'utf8');
const testSource = await fs.readFile(path.join(root, TEST), 'utf8');
const cases = [
  { id: 'control', description: 'Unmodified production code' },
  { id: 'accept-nonpositive-points', description: 'Remove zero/negative rejection', from: 'amount == null || amount <= 0', to: 'amount == null' },
  { id: 'duplicate-collection', description: 'Check existence but ignore duplicate result',
    from: 'if (doodyCollectionRepository.existsByUser_IdAndDoodyTemplate_Id(user.getId(), template.getId())) {\n            return Optional.empty();\n        }',
    to: 'doodyCollectionRepository.existsByUser_IdAndDoodyTemplate_Id(user.getId(), template.getId());' },
  { id: 'wrong-default-tier', description: 'Replace template tier with an incorrect constant', from: 'tier == null ? template.getTier() : tier', to: 'tier == null ? "WRONG" : tier' },
  { id: 'lost-point-origin', description: 'Drop point source ID', from: 'PointTransaction.earn(user, amount, reason, sourceType, sourceId)', to: 'PointTransaction.earn(user, amount, reason, sourceType, null)' },
  { id: 'wrong-contact-axis', description: 'Overwrite provided contact axis', from: '                contactId,\n                axis,', to: '                contactId,\n                "WRONG",' },
];
const results = [];
for (const item of cases) {
  const workspace = path.join(output, item.id);
  if (!refresh) {
    await fs.mkdir(workspace);
    for (const name of ['src', 'gradle', 'build.gradle', 'settings.gradle', 'gradlew', 'gradlew.bat']) {
      await fs.cp(path.join(root, name), path.join(workspace, name), { recursive: true });
    }
  }
  let source = original.replace(/\r\n/g, '\n');
  if (item.from) {
    if (source.split(item.from).length !== 2) throw new Error(`Mutation anchor must occur exactly once: ${item.id}`);
    source = source.replace(item.from, item.to);
  }
  if (!refresh) await fs.writeFile(path.join(workspace, SERVICE), source);
  console.log(`${refresh ? 'Re-reading XML for' : 'Running'} ${item.id}`);
  const result = refresh ? previous.cases.find(c => c.id === item.id)?.result : await runGradle(workspace, path.join(output, `${item.id}.log`));
  if (!result) throw new Error(`No cached execution: ${item.id}`);
  if (refresh) {
    if (previous.testHash !== sha(testSource)) throw new Error('Tests changed since evaluation');
    result.suites = [parseJUnit(await fs.readFile(path.join(workspace, 'build/test-results/test', `TEST-${TEST_CLASS}.xml`), 'utf8'))];
    result.classification = classifyRun(result.exitCode, await fs.readFile(path.join(output, `${item.id}.log`), 'utf8'), result.suites, result.timedOut);
  }
  const assertionFailed = result.suites.some(s => s.failedTests.some(f => /AssertionError|AssertionFailedError|NoInteractionsWanted|NeverWantedButInvoked|WantedButNotInvoked/.test(f.details)));
  results.push({ id: item.id, description: item.description, mutatedSourceHash: sha(source),
    detected: item.id === 'control' ? null : result.classification === 'test_failure' && assertionFailed,
    result,
  });
  await fs.writeFile(path.join(output, 'evaluation.json'), JSON.stringify({ testHash: sha(testSource), productionHash: sha(original), cases: results }, null, 2));
  console.log(JSON.stringify({ id: item.id, classification: result.classification, tests: result.tests, failures: result.failures, assertionFailed }));
}
if (sha(await fs.readFile(path.join(root, SERVICE), 'utf8')) !== sha(original)) throw new Error('Original source changed');
const controlPassed = results[0].result.classification === 'passed';
const detected = results.slice(1).filter(r => controlPassed && r.detected).length;
const report = `# Reward test defect detection evaluation\n\nControl passed: ${controlPassed}\nSelected injected defects detected: ${detected}/${cases.length - 1}\nOriginal service unchanged: true\nTest SHA256: ${sha(testSource)}\n\n| Case | Classification | Tests | Failures | Detected by assertion |\n| --- | --- | --- | --- | --- |\n${results.map(r => `| ${r.id} | ${r.result.classification} | ${r.result.tests} | ${r.result.failures} | ${r.detected ?? 'control'} |`).join('\n')}\n\nThese are five selected mutations, not broad mutation coverage or proof of production correctness. Compilation/environment failures do not count as detections. Mockito assertion failures count; incidental exceptions alone do not. Each case runs in a separate copy; production sources are not edited.\n`;
await fs.writeFile(path.join(output, 'report.md'), report);
console.log(report);
if (!controlPassed || detected !== cases.length - 1) process.exitCode = 1;
