import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';
import { instructions, actionSchema, executeAction, renderReport, sha, SERVICE, safePath } from './core.mjs';
import { requestAction } from './providers.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const [command, ...argv] = process.argv.slice(2);
function option(name, fallback) { const i = argv.indexOf(`--${name}`); return i < 0 ? fallback : argv[i + 1]; }
const runName = option('run', 'reward-demo');
if (!/^[a-zA-Z0-9_-]{1,64}$/.test(runName)) throw new Error('Invalid run name');
const runDir = await safePath(root, `.agent-runs/${runName}`);
const statePath = path.join(runDir, 'state.json');
async function save(state) {
  await fs.writeFile(statePath, JSON.stringify(state, null, 2));
  await fs.writeFile(path.join(runDir, 'report.md'), renderReport(state));
  await fs.writeFile(path.join(runDir, 'request.json'), JSON.stringify({ instructions, actionSchema, input: state.input }, null, 2));
}
async function newState(provider) {
  await fs.mkdir(runDir, { recursive: true });
  if (await fs.stat(statePath).then(() => true, () => false)) throw new Error('Run already exists: choose a new --run');
  return {
    version: 1, provider, model: provider === 'openai' ? process.env.OPENAI_MODEL : 'current-chat-model',
    startedAt: new Date().toISOString(), status: 'running', actions: 0, writes: 0, runs: 0, readFiles: [], trace: [],
    requiresDocs: argv.includes('--with-docs'), documentWrites: 0,
    productionHash: sha(await fs.readFile(path.join(root, SERVICE))),
    input: [{ role: 'user', content: 'Build and execute reward service regression tests from R01-R08. Inspect actual APIs, report failures honestly, and finish with verified results.' + (argv.includes('--with-docs') ? ' Also read all collection capture API sources and generate Korean API documentation using write_api_doc after test execution. Include HTTP contract test results and distinguish source-only rules.' : '') }],
  };
}
async function step(state, action, apiItem = null) {
  if (state.status !== 'running') throw new Error('Run has already ended');
  if (sha(await fs.readFile(path.join(root, SERVICE))) !== state.productionHash) throw new Error('Production service changed during the run');
  const call = apiItem || { type: 'function_call', name: 'agent_action', call_id: `session_${randomUUID()}`, arguments: JSON.stringify(action) };
  if (!apiItem) state.input.push(call);
  let result;
  try { result = await executeAction(root, runDir, state, action); state.trace.push({ at: new Date().toISOString(), action, result }); }
  catch (e) { result = { error: e.message }; state.trace.push({ at: new Date().toISOString(), action, error: e.message }); if (state.actions >= 20) state.status = 'needs_attention'; }
  state.input.push({ type: 'function_call_output', call_id: call.call_id, output: JSON.stringify(result) });
  await save(state);
  return result;
}

if (command === 'start') {
  const state = await newState('session');
  await save(state);
  console.log(JSON.stringify({ runDir, request: path.join(runDir, 'request.json'), instructions: 'Model reads request.json, writes a six-field JSON action, then invoke step --action <relative-file>.' }, null, 2));
} else if (command === 'step') {
  const state = JSON.parse(await fs.readFile(statePath, 'utf8'));
  if (state.provider !== 'session') throw new Error('step is for session mode only');
  const action = option('tool') ? {
    tool: option('tool'), reason: option('reason', ''), path: option('path', ''), query: option('query', ''), summary: option('summary', ''),
    content: option('source') ? await fs.readFile(await safePath(root, option('source')), 'utf8') : '',
  } : JSON.parse((await fs.readFile(await safePath(root, option('action', '')), 'utf8')).replace(/^\uFEFF/, ''));
  console.log(JSON.stringify(await step(state, action), null, 2));
} else if (command === 'auto') {
  if (!process.env.OPENAI_API_KEY || !process.env.OPENAI_MODEL) throw new Error('Set OPENAI_API_KEY and OPENAI_MODEL in the environment. No key is stored by this app.');
  const state = await newState('openai');
  await save(state);
  while (state.status === 'running' && state.actions < 20) {
    try {
      const next = await requestAction(state, { key: process.env.OPENAI_API_KEY });
      state.input.push(...next.output);
      const result = await step(state, next.action, next.call);
      console.log(JSON.stringify({ step: state.actions, tool: next.action.tool, classification: result.classification, error: result.error }));
    } catch (error) {
      state.status = 'needs_attention';
      state.summary = `Provider/execution stopped: ${error.message.replaceAll(process.env.OPENAI_API_KEY, '[redacted]')}`;
      await save(state);
      throw new Error(state.summary);
    }
  }
  if (state.status === 'running') { state.status = 'needs_attention'; state.summary = 'Action budget exhausted.'; await save(state); }
  console.log(`Report: ${path.join(runDir, 'report.md')}`);
} else {
  console.log('node tools/test-agent/cli.mjs start|step|auto --run NAME [--action RELATIVE_JSON]\nSet JAVA_HOME to JDK 21. auto also requires OPENAI_API_KEY and OPENAI_MODEL.');
}
