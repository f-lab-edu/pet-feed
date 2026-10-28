/* Temporary client: demo data stays local; API requests use the Gateway origin. */
'use strict';
const $ = (selector, root = document) => root.querySelector(selector);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const safeImage = value => { try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : ''; } catch { return ''; } };
function readStorage(storage, key, fallback) { try { return JSON.parse(storage.getItem(key)) ?? fallback; } catch { return fallback; } }
function persist(storage, key, value) { try { storage.setItem(key, JSON.stringify(value)); } catch { /* Private browsing can disable storage. */ } }
const seed = () => ({
  posts: [
    {postId: 3, userKey: 'demo-me', author: {displayName: '모카와 나'}, content: '산책이라는 말에 귀가 쫑긋. 🐾\n오늘도 모카 덕분에 조금 더 걸었어요. 가을은 천천히 같이 걷기 좋은 계절이네요.', art: 'park', emoji: '🐕', createdAt: new Date(Date.now() - 1800000).toISOString()},
    {postId: 2, userKey: 'demo-bori', author: {displayName: '보리의 오후'}, content: '햇살이 제일 잘 드는 자리는 언제나 보리 차지.\n아무것도 안 해도 좋은 오후예요. ☁', art: 'nap', emoji: '🐈', createdAt: new Date(Date.now() - 7200000).toISOString()},
    {postId: 1, userKey: 'demo-me', author: {displayName: '모카와 나'}, content: '우리의 첫 번째 기록.\n별일 없는 하루도 함께라서 특별하니까요. 🌿', createdAt: new Date(Date.now() - 86400000).toISOString()}
  ],
  comments: [
    {commentId: 2, postId: 3, userKey: 'demo-bori', author: {displayName: '보리의 오후'}, content: '귀가 너무 귀여워요! 오늘도 즐거운 산책 🐾', createdAt: new Date().toISOString()},
    {commentId: 1, postId: 3, userKey: 'demo-me', author: {displayName: '모카와 나'}, content: '이 길이 모카의 최애 산책 코스예요.', createdAt: new Date().toISOString()}
  ]
});
let demoData = readStorage(localStorage, 'petfeed-demo-v1', null);
if (!demoData || !Array.isArray(demoData.posts) || !Array.isArray(demoData.comments)) demoData = seed();
const state = { mode: 'demo', token: readStorage(sessionStorage, 'petfeed-token', ''), userKey: '', target: '', posts: [], cursor: 0, busy: false, generation: 0, commentPages: new Map(), editor: null, signup: false, deletion: null };
function tokenKey(token) {
  try { const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'); return JSON.parse(atob(part)).sub || ''; } catch { return ''; }
}
state.userKey = tokenKey(state.token);
const me = () => state.mode === 'demo' ? 'demo-me' : state.userKey;
const saveDemo = () => persist(localStorage, 'petfeed-demo-v1', demoData);
const nameOf = item => item.author?.displayName || item.userKey || '이름 없는 친구';
function timeOf(value) {
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return '';
  return date.toLocaleString('ko-KR', {month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'});
}
function notice(message = '', error = false) { const node = $('#notice'); node.textContent = message; node.hidden = !message; node.classList.toggle('error', error); }
async function api(path, method = 'GET', body) {
  const headers = {Accept: 'application/json'};
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try { response = await fetch(`/api/v1${path}`, {method, headers, body: body === undefined ? undefined : JSON.stringify(body)}); }
  catch { throw new Error('서버에 연결할 수 없어요. Gateway와 각 서비스 실행 상태를 확인해 주세요.'); }
  const raw = await response.text();
  let data; try { data = raw ? JSON.parse(raw) : null; } catch { data = null; }
  if (!response.ok) {
    const messages = {400: '입력 내용을 확인해 주세요.', 401: '로그인이 필요하거나 세션이 만료됐어요.', 403: '접근 권한이 없어요. 로그인 상태와 작성자를 확인해 주세요.', 404: '해당 기록이나 회원을 찾을 수 없어요.', 409: '이미 사용 중인 정보예요.'};
    throw new Error(messages[response.status] || data?.detail || data?.message || `요청에 실패했어요. (${response.status})`);
  }
  return data;
}
function syncChrome() {
  $('#mode-badge').textContent = state.mode === 'demo' ? '데모 모드' : '실제 API';
  $('#mode-button').textContent = state.mode === 'demo' ? '실제 API로 전환 ↗' : '데모 모드로 돌아가기 ↗';
  $('#account-button').textContent = state.mode === 'api' && state.token ? '로그아웃' : '로그인';
  $('#demo-note').textContent = state.mode === 'demo' ? '데모 기록은 이 브라우저에 저장돼요.' : '현재 Gateway의 실제 API를 사용해요.';
  $('#user-key').value = state.target;
  $('#feed-title').textContent = state.target === me() ? '나의 기록 ☀' : '우리들의 기록 ☀';
  $('#home-button').classList.toggle('active', state.target !== me());
  $('#mine-button').classList.toggle('active', state.target === me());
  $('#feed-description').textContent = state.mode === 'demo' ? '함께 나누는 오늘의 순간' : state.target ? `회원 ${state.target}의 기록` : '로그인 후 내 기록을 확인해 보세요';
}
function renderPosts() {
  $('#feed').innerHTML = state.posts.map(post => {
    const mine = post.userKey === me();
    const image = safeImage(post.thumbnailUrl);
    const count = state.mode === 'demo' ? demoData.comments.filter(c => c.postId === post.postId).length : post.commentCount ?? 0;
    return `<article class="post-card" data-post="${esc(post.postId)}">
      <div class="post-header"><div class="avatar">${esc(nameOf(post).slice(0, 1))}</div><div><div class="post-author">${esc(nameOf(post))}</div><div class="post-time">${esc(timeOf(post.createdAt))}</div></div>${mine ? '<div class="post-actions"><button class="icon-button" data-action="edit-post">수정</button><button class="icon-button" data-action="delete-post">삭제</button></div>' : ''}</div>
      ${image ? `<div class="post-image"><img src="${esc(image)}" alt="게시물 사진" loading="lazy" referrerpolicy="no-referrer"></div>` : post.art ? `<div class="post-image ${esc(post.art)}" aria-label="데모 일러스트"><span class="pet-emoji">${esc(post.emoji)}</span><span class="art-label">LITTLE MOMENTS, BIG LOVE</span></div>` : ''}
      <div class="post-content"><p>${esc(post.content)}</p></div><div class="post-footer"><button class="comment-toggle" data-action="comments" aria-expanded="false">☏ 댓글 ${count} <span>· 이야기 나누기</span></button><span class="post-id">RECORD ${String(post.postId).padStart(3, '0')}</span></div><div class="comments" hidden></div></article>`;
  }).join('') || '<div class="empty-state"><span>🌱</span>아직 남긴 기록이 없어요.<br>첫 번째 순간을 남겨보세요.</div>';
  $('#more-posts').hidden = !state.cursor;
  $('#feed').querySelectorAll('img').forEach(img => img.addEventListener('error', () => { img.closest('.post-image').hidden = true; }));
}
async function loadPosts(append = false) {
  const generation = ++state.generation;
  const cursor = append ? state.cursor : 0;
  $('#feed').setAttribute('aria-busy', 'true');
  $('#more-posts').disabled = true;
  notice();
  syncChrome();
  try {
    let posts, next;
    if (state.mode === 'demo') {
      posts = demoData.posts.filter(post => !state.target || post.userKey === state.target).sort((a,b) => b.postId - a.postId);
      next = null;
    } else {
      if (!state.token || !state.target) {
        state.posts = []; state.cursor = null; renderPosts();
        notice(state.token ? '조회할 회원 키를 입력해 주세요.' : '실제 데이터를 보려면 로그인해 주세요.');
        return;
      }
      const page = await api(`/users/${encodeURIComponent(state.target)}/posts?cursor=${cursor}`);
      posts = await Promise.all(page.posts.map(async summary => ({...summary, ...await api(`/posts/${summary.postId}`)})));
      next = page.hasNext ? page.nextCursor : null;
    }
    if (generation !== state.generation) return;
    state.posts = append ? [...state.posts, ...posts] : posts;
    state.cursor = next; state.commentPages.clear(); renderPosts();
  } catch (error) { if (generation === state.generation) { state.posts = []; state.cursor = null; renderPosts(); notice(error.message, true); } }
  finally { if (generation === state.generation) { $('#feed').setAttribute('aria-busy', 'false'); $('#more-posts').disabled = false; } }
}
function commentMarkup(comment) {
  return `<div class="comment" data-comment="${esc(comment.commentId)}"><div class="avatar">${esc(nameOf(comment).slice(0,1))}</div><div class="comment-main"><div class="comment-meta"><strong>${esc(nameOf(comment))}</strong><time>${esc(timeOf(comment.createdAt))}</time>${comment.userKey === me() ? '<button class="icon-button" data-action="edit-comment">수정</button><button class="icon-button" data-action="delete-comment">삭제</button>' : ''}</div><p>${esc(comment.content)}</p></div></div>`;
}
async function loadComments(card, append = false) {
  const postId = Number(card.dataset.post);
  const node = $('.comments', card);
  node.hidden = false;
  const toggle = $('.comment-toggle', card);
  toggle.setAttribute('aria-expanded', 'true');
  toggle.disabled = true;
  try {
    const previous = state.commentPages.get(postId);
    let page;
    if (state.mode === 'demo') page = {comments: demoData.comments.filter(c => c.postId === postId).sort((a,b) => b.commentId - a.commentId), hasNext: false};
    else page = await api(`/posts/${postId}/comments?cursor=${append ? previous?.nextCursor || 0 : 0}`);
    if (!card.isConnected) return;
    if (append) page.comments = [...previous.comments, ...page.comments];
    state.commentPages.set(postId, page);
    node.innerHTML = `<div class="comment-list">${page.comments.map(commentMarkup).join('') || '<div class="comment-empty">첫 번째 이야기를 남겨주세요 🌿</div>'}</div>${page.hasNext ? '<button class="button quiet" data-action="more-comments">댓글 더 보기 ↓</button>' : ''}<form class="comment-form"><label class="sr-only" for="comment-${postId}">댓글 내용</label><textarea id="comment-${postId}" rows="1" maxlength="1000" required placeholder="다정한 한마디를 남겨주세요."></textarea><button class="button primary" type="submit">등록 ↗</button></form><p class="form-error" role="alert"></p>`;
  } catch (error) { notice(error.message, true); } finally { toggle.disabled = false; }
}
function canWrite() {
  if (state.mode === 'api' && !state.token) { openAuth(); return false; }
  return true;
}
function openEditor(kind, postId, commentId) {
  if (!canWrite()) return;
  const post = state.posts.find(p => p.postId === postId);
  const comment = state.commentPages.get(postId)?.comments.find(c => c.commentId === commentId);
  state.editor = {kind, postId, commentId};
  $('#editor-title').textContent = kind === 'comment' ? '댓글 수정하기' : postId ? '순간 수정하기' : '순간 남기기';
  $('#editor-content').maxLength = kind === 'comment' ? 1000 : 5000;
  $('#editor-content').value = kind === 'comment' ? comment.content : post?.content || '';
  $('#thumbnail-field').hidden = kind === 'comment';
  $('#editor-thumbnail').value = post?.thumbnailUrl || '';
  $('#editor-thumbnail').disabled = kind === 'comment';
  $('#editor-error').textContent = '';
  $('#editor-dialog').showModal();
}
$('#editor-form').addEventListener('submit', async event => {
  event.preventDefault();
  const content = $('#editor-content').value;
  if (!content.trim()) { $('#editor-error').textContent = '내용을 입력해 주세요.'; return; }
  const {kind, postId, commentId} = state.editor;
  const thumbnailUrl = $('#editor-thumbnail').value.trim() || null;
  if (kind === 'post' && thumbnailUrl && !safeImage(thumbnailUrl)) { $('#editor-error').textContent = 'http 또는 https 사진 URL을 입력해 주세요.'; return; }
  $('#editor-submit').disabled = true;
  try {
    if (state.mode === 'demo') {
      if (kind === 'comment') Object.assign(demoData.comments.find(c => c.commentId === commentId), {content, updatedAt: new Date().toISOString()});
      else if (postId) Object.assign(demoData.posts.find(p => p.postId === postId), {content, thumbnailUrl, updatedAt: new Date().toISOString()});
      else demoData.posts.unshift({postId: Date.now(), userKey: me(), author: {displayName: '모카와 나'}, content, thumbnailUrl, createdAt: new Date().toISOString()});
      saveDemo();
    } else {
      const path = kind === 'comment' ? `/posts/${postId}/comments/${commentId}` : postId ? `/posts/${postId}` : '/posts';
      await api(path, postId ? 'PUT' : 'POST', kind === 'comment' ? {content} : {content, thumbnailUrl});
    }
    $('#editor-dialog').close();
    if (kind === 'comment') await loadComments($(`[data-post="${postId}"]`));
    else { state.target = me(); await loadPosts(); }
  } catch (error) { $('#editor-error').textContent = error.message; }
  finally { $('#editor-submit').disabled = false; }
});
$('#feed').addEventListener('submit', async event => {
  if (!event.target.matches('.comment-form')) return;
  event.preventDefault();
  if (!canWrite()) return;
  const form = event.target, card = form.closest('.post-card'), postId = Number(card.dataset.post);
  const content = $('textarea', form).value;
  if (!content.trim()) return;
  $('button', form).disabled = true;
  try {
    if (state.mode === 'demo') {
      demoData.comments.push({commentId: Date.now(), postId, userKey: me(), author: {displayName:'모카와 나'}, content, createdAt: new Date().toISOString()}); saveDemo();
    } else await api(`/posts/${postId}/comments`, 'POST', {content});
    const post = state.posts.find(p => p.postId === postId);
    post.commentCount = (post.commentCount || 0) + 1;
    updateCount(card);
    await loadComments(card);
  } catch (error) { $('.form-error', card).textContent = error.message; }
  finally { if (form.isConnected) $('button', form).disabled = false; }
});
function updateCount(card) {
  const postId = Number(card.dataset.post);
  const count = state.mode === 'demo' ? demoData.comments.filter(c => c.postId === postId).length : state.posts.find(p => p.postId === postId).commentCount;
  $('.comment-toggle', card).textContent = `☏ 댓글 ${count} · 이야기 나누기`;
}
$('#feed').addEventListener('click', async event => {
  const button = event.target.closest('[data-action]');
  if (!button) return;
  const card = button.closest('.post-card'), postId = Number(card.dataset.post);
  const commentId = Number(button.closest('[data-comment]')?.dataset.comment);
  const action = button.dataset.action;
  if (action === 'comments') {
    const node = $('.comments', card);
    if (!node.hidden) { node.hidden = true; button.setAttribute('aria-expanded', 'false'); }
    else await loadComments(card);
  } else if (action === 'more-comments') {
    button.disabled = true;
    await loadComments(card, true);
    if (button.isConnected) button.disabled = false;
  } else if (action.startsWith('edit-')) openEditor(action === 'edit-post' ? 'post' : 'comment', postId, commentId);
  else if (action.startsWith('delete-')) {
    state.deletion = {postId, commentId: action === 'delete-comment' ? commentId : null};
    $('#confirm-dialog').showModal();
  }
});
$('#confirm-delete').addEventListener('click', async () => {
  const {postId, commentId} = state.deletion;
  $('#confirm-delete').disabled = true;
  try {
    if (state.mode === 'demo') {
      if (commentId) demoData.comments = demoData.comments.filter(c => c.commentId !== commentId);
      else { demoData.posts = demoData.posts.filter(p => p.postId !== postId); demoData.comments = demoData.comments.filter(c => c.postId !== postId); }
      saveDemo();
    } else await api(commentId ? `/posts/${postId}/comments/${commentId}` : `/posts/${postId}`, 'DELETE');
    $('#confirm-dialog').close();
    if (commentId) {
      const card = $(`[data-post="${postId}"]`);
      const post = state.posts.find(p => p.postId === postId); post.commentCount = Math.max(0, (post.commentCount || 0) - 1);
      updateCount(card); await loadComments(card);
    } else await loadPosts();
  } catch (error) { $('#confirm-dialog').close(); notice(error.message, true); }
  finally { $('#confirm-delete').disabled = false; }
});
function openAuth() {
  state.signup = false; syncAuth(); $('#auth-error').textContent = ''; $('#auth-dialog').showModal();
}
function syncAuth() {
  $('#signup-fields').hidden = !state.signup;
  $('#nickname').required = state.signup; $('#phone').required = state.signup;
  $('#auth-title').textContent = state.signup ? '우리의 기록을 시작해요' : '다시 만나 반가워요';
  $('#auth-submit').textContent = state.signup ? '회원가입' : '로그인';
  $('#auth-toggle').textContent = state.signup ? '이미 계정이 있어요 · 로그인' : '처음인가요? 회원가입';
  $('#password').autocomplete = state.signup ? 'new-password' : 'current-password';
}
$('#auth-toggle').addEventListener('click', () => { state.signup = !state.signup; $('#auth-error').textContent = ''; syncAuth(); });
$('#auth-form').addEventListener('submit', async event => {
  event.preventDefault(); $('#auth-submit').disabled = true; $('#auth-error').textContent = '';
  try {
    const body = {email: $('#email').value, password: $('#password').value};
    if (state.signup) {
      await api('/users/signup', 'POST', {...body, nickname: $('#nickname').value, phone: $('#phone').value});
      state.signup = false; syncAuth(); $('#auth-error').textContent = '가입했어요. 같은 계정으로 로그인해 주세요.';
    } else {
      const response = await api('/auth/login', 'POST', body);
      const key = tokenKey(response.accessToken);
      if (!key) throw new Error('로그인 응답의 사용자 정보를 확인할 수 없어요.');
      state.token = response.accessToken; state.userKey = key; state.target = key; state.mode = 'api';
      persist(sessionStorage, 'petfeed-token', state.token);
      $('#password').value = ''; $('#auth-dialog').close(); await loadPosts();
    }
  } catch (error) { $('#auth-error').textContent = error.message; }
  finally { $('#auth-submit').disabled = false; }
});
$('#account-button').addEventListener('click', () => {
  if (state.mode === 'api' && state.token) { state.token = ''; state.userKey = ''; state.target = ''; persist(sessionStorage, 'petfeed-token', ''); loadPosts(); }
  else openAuth();
});
$('#mode-button').addEventListener('click', () => { state.mode = state.mode === 'demo' ? 'api' : 'demo'; state.target = state.mode === 'demo' ? '' : state.userKey; loadPosts(); });
$('#compose-button').addEventListener('click', () => openEditor('post'));
$('#write-button').addEventListener('click', () => openEditor('post'));
$('#refresh-button').addEventListener('click', () => loadPosts());
$('#more-posts').addEventListener('click', () => loadPosts(true));
$('#home-button').addEventListener('click', () => { state.target = state.mode === 'demo' ? '' : me(); loadPosts(); });
$('#mine-button').addEventListener('click', () => { if (!canWrite()) return; state.target = me(); loadPosts(); });
$('#search-form').addEventListener('submit', event => { event.preventDefault(); state.target = $('#user-key').value.trim(); loadPosts(); });
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
loadPosts();
