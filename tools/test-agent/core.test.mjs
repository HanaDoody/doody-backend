import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { parseJUnit, classifyRun, safePath, executeAction, SERVICE, SPEC, API_SOURCES, API_DOC } from './core.mjs';

async function removeFixture(target, prefix) {
  const resolved = await fs.realpath(target);
  const tempRoot = await fs.realpath(os.tmpdir());
  if (path.dirname(resolved) !== tempRoot || !path.basename(resolved).startsWith(prefix)) throw new Error('Unexpected cleanup target');
  await fs.rm(resolved, { recursive: true, force: true });
}

test('JUnit failures retain failing test and decoded assertion', () => {
  const suite = parseJUnit('<testsuite name="Rewards" tests="3" failures="1" errors="0" skipped="0"><testcase name="earlierPass()"/><testcase name="rejectsNegative()"><failure type="AssertionError">expected &lt;0&gt; but got 1</failure></testcase><testcase name="ok()"/></testsuite>');
  assert.equal(suite.tests, 3);
  assert.equal(suite.failedTests[0].test, 'rejectsNegative()');
  assert.match(suite.failedTests[0].details, /expected <0>/);
  assert.equal(classifyRun(1, 'test failed', [suite]), 'test_failure');
});
test('compile errors, environment failures and zero tests never become success', () => {
  assert.equal(classifyRun(1, "Execution failed for task ':compileTestJava'", []), 'compilation');
  assert.equal(classifyRun(1, 'Could not resolve dependencies', []), 'environment_or_build');
  assert.equal(classifyRun(0, 'UP-TO-DATE', []), 'no_tests');
  assert.equal(classifyRun(0, '', [{ tests: 2, failures: 0, errors: 0, skipped: 2 }]), 'no_tests');
  assert.equal(classifyRun(0, '', [{ tests: 2, failures: 0, errors: 0 }]), 'passed');
  assert.equal(classifyRun(-1, '', [], true), 'timeout');
});
test('path policy rejects traversal, absolute paths and symlink escape', async () => {
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'doody-path-test-'));
  const root = path.join(dir, 'root'), outside = path.join(dir, 'outside');
  await fs.mkdir(root); await fs.mkdir(outside);
  try {
    for (const p of ['../outside', 'C:/Windows', '/etc/passwd', 'a\\b']) await assert.rejects(safePath(root, p));
    await fs.symlink(outside, path.join(root, 'link'), process.platform === 'win32' ? 'junction' : 'dir');
    await assert.rejects(safePath(root, 'link/new.java'), /Symlink/);
    assert.equal(await safePath(root, 'new/file.java'), path.join(root, 'new/file.java'));
  } finally { await removeFixture(dir, 'doody-path-test-'); }
});
test('actions cannot write production files, skip source inspection, or exceed repair budget', async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'doody-action-test-'));
  const state = { actions: 0, readFiles: [], writes: 0, runs: 0 };
  const action = { tool: 'write_test', reason: 'test boundary', path: SERVICE, content: '', query: '', summary: '' };
  try {
    await assert.rejects(executeAction(root, root, state, action), /Read service/);
    await assert.rejects(executeAction(root, root, state, { ...action, tool: 'read_file', path: '.env' }), /Read restricted/);
    state.readFiles = [SERVICE, SPEC]; state.writes = 3;
    await assert.rejects(executeAction(root, root, state, action), /two repairs/);
    await assert.rejects(executeAction(root, root, state, { ...action, tool: 'finish', summary: 'done' }), /test execution/);
    state.actions = 20;
    await assert.rejects(executeAction(root, root, state, action), /budget/);
  } finally { await removeFixture(root, 'doody-action-test-'); }
});

test('API documentation requires executed tests and all API sources', async () => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'doody-doc-test-'));
  const state = { actions: 0, readFiles: [], writes: 1, runs: 1, testHash: 'test-hash', requiresDocs: true };
  const action = { tool: 'write_api_doc', reason: 'document observed contract', path: '', content: '# POST /api/collection/capture\n## 요청\n## 응답\n## 오류\n## 검증 범위\n## 미검증 범위', query: '', summary: '' };
  try {
    await assert.rejects(executeAction(root, root, state, action), /passing test/);
    state.lastRun = { classification: 'passed', tests: 24, failures: 0, endedAt: 'test-time' };
    await assert.rejects(executeAction(root, root, state, action), /Read controller/);
    state.readFiles = [...API_SOURCES];
    await assert.rejects(executeAction(root, root, state, { ...action, tool: 'finish', summary: 'done' }), /generated API/);
    const result = await executeAction(root, root, state, action);
    assert.equal(result.path, API_DOC);
    const doc = await fs.readFile(path.join(root, API_DOC), 'utf8');
    assert.match(doc, /agent-evidence/);
    assert.match(doc, /"tests":24/);
    state.documentWrites = 3;
    await assert.rejects(executeAction(root, root, state, action), /repair budget/);
  } finally { await removeFixture(root, 'doody-doc-test-'); }
});
