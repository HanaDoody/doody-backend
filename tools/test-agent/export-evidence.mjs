import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseJUnit, sha, SERVICE, TEST, API_DOC } from './core.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const out = path.join(root, 'docs/test-agent');
const run = JSON.parse(await fs.readFile(path.join(root, '.agent-runs/reward-demo/state.json'), 'utf8'));
const docRun = JSON.parse(await fs.readFile(path.join(root, '.agent-runs/reward-docs/state.json'), 'utf8'));
const evaluation = JSON.parse(await fs.readFile(path.join(root, '.agent-runs/reward-evaluation/evaluation.json'), 'utf8'));
const sourceHash = sha(await fs.readFile(path.join(root, SERVICE)));
const testHash = sha(await fs.readFile(path.join(root, TEST)));
if (sourceHash !== run.productionHash || sourceHash !== evaluation.productionHash || testHash !== evaluation.testHash || testHash !== run.testHash) throw new Error('Source or test does not match recorded evaluation');
if (docRun.document?.sha256 !== sha(await fs.readFile(path.join(root, API_DOC))) || docRun.status !== 'completed') throw new Error('API document does not match completed run');
const suites = [];
const testDir = path.join(root, 'build/test-results/test');
for (const name of await fs.readdir(testDir)) if (/^TEST-.*\.xml$/.test(name)) suites.push(parseJUnit(await fs.readFile(path.join(testDir, name), 'utf8')));
const total = suites.reduce((n, s) => n + s.tests, 0);
const failures = suites.reduce((n, s) => n + s.failures + s.errors, 0);
const skipped = suites.reduce((n, s) => n + s.skipped, 0);
if (total !== 35 || failures || skipped) throw new Error('Expected 35 passing tests from final full build');
await fs.mkdir(out, { recursive: true });
await fs.writeFile(path.join(out, 'verification-results.json'), JSON.stringify({
  recordedAt: new Date().toISOString(), dateZone: 'Asia/Seoul',
  javaVersion: '21.0.10', nodeVersion: process.version,
  buildCommand: '.\\gradlew.bat clean build --no-daemon --console=plain',
  buildExitCode: 0, javaTests: total, javaFailures: failures, javaSkipped: skipped, suites,
  generatedTestCount: run.lastRun.tests, agentActions: run.actions, generatedVersions: run.writes,
  targetedTestExecutions: run.runs, provider: run.provider, model: run.model,
  liveApiValidated: false, productionHash: sourceHash, testHash,
  documentationRun: {
    document: docRun.document, targetedTests: docRun.lastRun.tests,
    failures: docRun.lastRun.failures, elapsedTestExecutionSeconds: (Date.parse(docRun.lastRun.endedAt) - Date.parse(docRun.lastRun.startedAt)) / 1000,
    httpContractTests: 4, source: '.agent-runs/reward-docs/state.json',
  },
}, null, 2));
await fs.writeFile(path.join(out, 'evaluation-results.json'), JSON.stringify({
  recordedAt: new Date().toISOString(), productionHash: sourceHash, testHash,
  controlPassed: evaluation.cases[0].result.classification === 'passed',
  selectedDefects: evaluation.cases.length - 1,
  detectedSelectedDefects: evaluation.cases.slice(1).filter(c => c.detected).length,
  cases: evaluation.cases.map(c => ({ id: c.id, description: c.description, detected: c.detected,
    classification: c.result.classification, tests: c.result.tests, failures: c.result.failures,
    startedAt: c.result.startedAt, endedAt: c.result.endedAt,
    failedTests: c.result.suites.flatMap(s => s.failedTests.map(f => ({ test: f.test, details: f.details.slice(0, 1200) }))),
  })),
}, null, 2));
for (const [source, name] of [['.agent-runs/reward-demo/report.md', 'session-report.md'], ['.agent-runs/reward-evaluation/report.md', 'evaluation-report.md'], ['.agent-runs/reward-docs/report.md', 'documentation-session-report.md']]) {
  await fs.copyFile(path.join(root, source), path.join(out, name));
}
console.log(JSON.stringify({ javaTests: total, failures, generatedTests: run.lastRun.tests, detected: evaluation.cases.slice(1).filter(c => c.detected).length, selectedDefects: evaluation.cases.length - 1, sourceUnchanged: true }, null, 2));
