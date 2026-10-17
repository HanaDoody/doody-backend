import { apiTool, instructions, validateAction } from './core.mjs';

export async function requestAction(state, { key, apiFetch = fetch } = {}) {
  if (!key || !state.model) throw new Error('API key and model are required');
  const response = await apiFetch('https://api.openai.com/v1/responses', {
    method: 'POST', headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: state.model, instructions, input: state.input, tools: [apiTool], tool_choice: 'required', parallel_tool_calls: false, store: false }),
    signal: AbortSignal.timeout(120000),
  });
  if (!response.ok) throw new Error(`API HTTP ${response.status}; response body omitted. Check model, account, or network settings.`);
  const data = await response.json();
  if (data.status !== 'completed' || !Array.isArray(data.output)) throw new Error(`API response did not complete: ${data.status}`);
  const calls = data.output.filter(item => item.type === 'function_call');
  if (calls.length !== 1 || calls[0].name !== 'agent_action') throw new Error('Expected exactly one agent_action');
  const action = JSON.parse(calls[0].arguments);
  validateAction(action);
  return { output: data.output, call: calls[0], action };
}
