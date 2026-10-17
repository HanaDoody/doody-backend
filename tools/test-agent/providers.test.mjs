import test from 'node:test';
import assert from 'node:assert/strict';
import { requestAction } from './providers.mjs';

const action = { tool: 'read_file', path: 'build.gradle', reason: 'inspect dependencies', content: '', query: '', summary: '' };
const call = { type: 'function_call', name: 'agent_action', call_id: 'test-call', arguments: JSON.stringify(action) };
test('Responses adapter sends tools and complete prior tool outputs without storing API key', async () => {
  const state = { model: 'configured-model', input: [{ type: 'function_call_output', call_id: 'prior', output: '{"tests":20}' }] };
  const next = await requestAction(state, { key: 'fake-test-key', apiFetch: async (url, options) => {
    assert.equal(url, 'https://api.openai.com/v1/responses');
    assert.equal(options.headers.Authorization, 'Bearer fake-test-key');
    const body = JSON.parse(options.body);
    assert.equal(body.store, false);
    assert.equal(body.parallel_tool_calls, false);
    assert.equal(body.input[0].call_id, 'prior');
    assert.equal(body.tools[0].name, 'agent_action');
    assert.equal(body.tools[0].strict, true);
    return { ok: true, json: async () => ({ status: 'completed', output: [call] }) };
  } });
  assert.deepEqual(next.action, action);
  assert.equal(JSON.stringify(state).includes('fake-test-key'), false);
});
test('provider HTTP errors, incomplete responses and unexpected tool calls are rejected', async () => {
  const state = { model: 'configured-model', input: [] };
  await assert.rejects(requestAction(state, { key: 'fake', apiFetch: async () => ({ ok: false, status: 401 }) }), /HTTP 401/);
  for (const payload of [{ status: 'incomplete', output: [] }, { status: 'completed', output: [] }, { status: 'completed', output: [call, call] }]) {
    await assert.rejects(requestAction(state, { key: 'fake', apiFetch: async () => ({ ok: true, json: async () => payload }) }));
  }
});
