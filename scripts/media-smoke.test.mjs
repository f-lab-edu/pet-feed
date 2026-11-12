import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {test} from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('./media-smoke.mjs', import.meta.url), 'utf8');
const requestSource = source.slice(source.indexOf("let token = ''"), source.indexOf("await request('/media'"));

function client() {
  const calls = [];
  const context = vm.createContext({assert, base: 'http://localhost:8080/api/v1',
    fetch: async (url, options) => { calls.push({url, options}); return {status: 200}; }
  });
  vm.runInContext(requestSource, context);
  return {calls, request: vm.runInContext('request', context)};
}

test('smoke requests accept local post/media routes and disable redirects', async () => {
  const {calls, request} = client();
  await request('/posts/123/media/12345678-1234-1234-1234-123456789abc');
  assert.equal(calls[0].url, 'http://localhost:8080/api/v1/posts/123/media/12345678-1234-1234-1234-123456789abc');
  assert.equal(calls[0].options.redirect, 'error');
});

test('smoke requests reject traversal, URLs, queries and invalid IDs before fetch', async () => {
  const {calls, request} = client();
  for (const path of ['/../admin', '/posts/../admin', '/posts/%2e%2e/admin',
    '//example.com', 'https://example.com', '/posts/1?redirect=evil', '/posts/0', '/posts/NaN', '/posts/1/media/../../admin']) {
    await assert.rejects(request(path));
  }
  assert.equal(calls.length, 0);
});
