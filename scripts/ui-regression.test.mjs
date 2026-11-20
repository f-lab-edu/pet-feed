import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {test} from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../apps/api-gateway/src/main/resources/static/ui/app.js', import.meta.url), 'utf8');

function boot(saved, blockedStorage = false) {
  const nodes = new Map();
  function node(selector) {
    if (!nodes.has(selector)) nodes.set(selector, {
      value: '', innerHTML: '', textContent: '', hidden: false, isConnected: true,
      dataset: {}, handlers: {}, classList: {toggle() {}},
      addEventListener(type, handler) { this.handlers[type] = handler; },
      setAttribute() {}, querySelectorAll() { return []; },
      querySelector(child) { return node(`${selector} ${child}`); },
      replaceChildren() {}, showModal() {}, close() {}
    });
    return nodes.get(selector);
  }
  const stored = new Map([['petfeed-demo-v1', JSON.stringify(saved)]]);
  const context = vm.createContext({
    document: {querySelector: node, querySelectorAll: () => []}, URL,
    localStorage: {
      getItem: key => stored.get(key),
      setItem(key, value) { if (blockedStorage) throw new Error('Storage disabled'); stored.set(key, value); }
    },
    sessionStorage: {getItem: () => null},
    fetch: async () => { throw new Error('Offline'); }
  });
  vm.runInContext(source, context);
  return {node, stored, setFetch: handler => { context.fetch = handler; }, run: code => vm.runInContext(code, context)};
}

test('legacy posts and comments retain ownership, editing, deletion and persistence', async () => {
  const app = boot({
    posts: [{postId: 7, userKey: 'demo-me', content: 'legacy post'}],
    comments: [{commentId: 9, postId: 7, userKey: 'demo-me', content: 'legacy comment'}]
  });
  await app.node('#mine-button').handlers.click();
  assert.match(app.node('#feed').innerHTML, /legacy post/);
  assert.match(app.node('#feed').innerHTML, /data-action="edit-post"/);
  assert.match(app.node('#feed').innerHTML, /data-action="delete-post"/);
  assert.match(app.run('commentMarkup(demoData.comments[0])'), /data-action="edit-comment"/);
  assert.match(app.run('commentMarkup(demoData.comments[0])'), /data-action="delete-comment"/);
  app.run("state.editor = {kind: 'post', postId: 7, attachments: []}");
  app.node('#editor-content').value = 'edited legacy post';
  await app.node('#editor-form').handlers.submit({preventDefault() {}});
  assert.match(app.node('#feed').innerHTML, /edited legacy post/);
  const persisted = JSON.parse(app.stored.get('petfeed-demo-v1'));
  assert.equal(persisted.posts[0].memberKey, 'demo-me');
  assert.equal(persisted.comments[0].memberKey, 'demo-me');
  const reloaded = boot(persisted);
  assert.match(reloaded.node('#feed').innerHTML, /edited legacy post/);
  app.run('state.deletion = {postId: 7, commentId: null}');
  await app.node('#confirm-delete').handlers.click();
  assert.deepEqual(JSON.parse(app.stored.get('petfeed-demo-v1')), {posts: [], comments: []});
});

test('mixed records keep current ownership and do not grant another member controls', async () => {
  const app = boot({posts: [
    {postId: 1, memberKey: 'demo-other', userKey: 'demo-me', content: 'other record'},
    {postId: 2, memberKey: 'demo-me', content: 'current record'},
    {postId: 3, content: 'unowned record'}
  ], comments: []});
  await app.node('#mine-button').handlers.click();
  assert.match(app.node('#feed').innerHTML, /current record/);
  assert.doesNotMatch(app.node('#feed').innerHTML, /other record|unowned record/);
});

test('migration works in memory when storage writes are disabled', async () => {
  const app = boot({posts: [{postId: 1, userKey: 'demo-me', content: 'private browsing'}], comments: []}, true);
  await app.node('#mine-button').handlers.click();
  assert.match(app.node('#feed').innerHTML, /private browsing/);
});

test('invalid saved data falls back to demo records', () => {
  const app = boot({posts: null});
  assert.equal(app.run('demoData.posts.length'), 3);
});

test('API failure finishes navigation and restores controls', async () => {
  const app = boot(null);
  app.run("state.mode = 'api'; state.memberKey = 'member'; state.token = 'token'");
  await app.node('#home-button').handlers.click();
  assert.match(app.node('#notice').textContent, /서버에 연결/);
  assert.equal(app.node('#more-posts').disabled, false);
});

test('home uses mixed feed and its opaque cursor without fetching each post again', async () => {
  const app = boot(null);
  const requests = [];
  app.setFetch(async url => {
    requests.push(url);
    return {ok: true, text: async () => JSON.stringify({
      items: [{post: {postId: requests.length, memberKey: 'friend', content: 'feed record'}, commentCount: 4, source: 'FOLLOWING'}],
      nextCursor: requests.length === 1 ? 'session:10' : null, hasNext: requests.length === 1
    })};
  });
  app.run("state.mode = 'api'; state.memberKey = 'me'; state.token = 'token'");
  await app.node('#home-button').handlers.click();
  await app.node('#more-posts').handlers.click();
  assert.deepEqual(requests, ['/api/v1/feed', '/api/v1/feed?cursor=session%3A10']);
  assert.equal(app.run('state.posts.length'), 2);
  assert.match(app.node('#feed').innerHTML, /댓글 4/);
  assert.equal(app.node('#more-posts').hidden, true);
  await app.node('#refresh-button').handlers.click();
  assert.equal(requests.at(-1), '/api/v1/feed');
});

test('expired feed gives a refresh instruction and clears the unusable cursor', async () => {
  const app = boot(null);
  app.setFetch(async () => ({ok: false, status: 410, text: async () => '{}'}));
  app.run("state.mode = 'api'; state.token = 'token'; state.cursor = 'expired:10'");
  await app.node('#more-posts').handlers.click();
  assert.match(app.node('#notice').textContent, /새로고침/);
  assert.equal(app.run('state.cursor'), null);
});
