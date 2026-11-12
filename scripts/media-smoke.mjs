// Local-only integration smoke test. Pipe MP4 bytes to stdin: node scripts/media-smoke.mjs < sample.mp4 (cmd/bash).
import {readFileSync} from 'node:fs';
import {deflateSync} from 'node:zlib';
import assert from 'node:assert/strict';

const base = 'http://localhost:8080/api/v1';
if (process.stdin.isTTY) throw new Error('Pipe an MP4 fixture to stdin. All seven services must be running.');
const video = readFileSync(0);
assert.ok(video.length > 0 && video.length <= 50 * 1024 * 1024, 'Provide an MP4 fixture of at most 50 MiB');

function png() {
  function chunk(type, data) {
    const body = Buffer.concat([Buffer.from(type), data]);
    let crc = 0xffffffff;
    for (const byte of body) {
      crc ^= byte;
      for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
    }
    const length = Buffer.alloc(4), checksum = Buffer.alloc(4);
    length.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
    return Buffer.concat([length, body, checksum]);
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(1, 0); header.writeUInt32BE(1, 4); header[8] = 8; header[9] = 2;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', header),
    chunk('IDAT', deflateSync(Buffer.from([0,80,160,90]))), chunk('IEND', Buffer.alloc(0))]);
}

let token = '';
async function request(path, {method = 'GET', body, type, headers = {}, expected = 200} = {}) {
  // Only the local smoke-test routes may receive requests, including server-returned IDs.
  assert.match(path, new RegExp("^/(?:users/signup|auth/login|media|posts(?:/[1-9]\\d*(?:/media/[0-9a-f-]{36})?)?)$"));
  const response = await fetch(base + path, {method, redirect: 'error',
    headers: {...(token ? {Authorization: `Bearer ${token}`} : {}),
      ...(body === undefined ? {} : {'Content-Type': type || 'application/json'}), ...headers},
    body: body === undefined ? undefined : type ? body : JSON.stringify(body)});
  assert.equal(response.status, expected, `${method} ${path}: ${response.status} ${response.status === expected ? '' : await response.text()}`);
  return response;
}

await request('/media', {method: 'POST', type: 'image/png', body: png(), expected: 403});
const nonce = Date.now();
const login = {email: `media-smoke-${nonce}@example.test`, password: `LocalTest!${nonce}`};
await request('/users/signup', {method: 'POST', body: {...login, nickname: '미디어 검증', phone: '01000000000'}, expected: 201});
token = (await (await request('/auth/login', {method: 'POST', body: login})).json()).accessToken;
assert.ok(token);
const photo = png();
const photoId = (await (await request('/media', {method: 'POST', type: 'image/png', body: photo, expected: 201})).json()).mediaId;
const videoId = (await (await request('/media', {method: 'POST', type: 'video/mp4', body: video, expected: 201})).json()).mediaId;
await request('/media', {method: 'POST', type: 'image/png', body: Buffer.from('not a photo'), expected: 415});
const post = await (await request('/posts', {method: 'POST', expected: 201,
  body: {content: '사진과 동영상 업로드 검증', mediaIds: [photoId, videoId]}})).json();
assert.deepEqual(post.mediaIds, [photoId, videoId]);
const postId = Number(post.postId);
assert.ok(Number.isSafeInteger(postId) && postId > 0, 'Expected a positive integer post ID');
assert.deepEqual(Buffer.from(await (await request(`/posts/${postId}/media/${encodeURIComponent(photoId)}`)).arrayBuffer()), photo);
const part = await request(`/posts/${postId}/media/${encodeURIComponent(videoId)}`, {headers: {Range: 'bytes=0-31'}, expected: 206});
assert.equal(part.headers.get('content-range'), `bytes 0-31/${video.length}`);
assert.deepEqual(Buffer.from(await part.arrayBuffer()), video.subarray(0, 32));
await request(`/posts/${postId}`, {method: 'PUT', body: {content: '첨부 교체', mediaIds: [videoId]}, expected: 204});
assert.deepEqual((await (await request(`/posts/${postId}`)).json()).mediaIds, [videoId]);
await request(`/posts/${postId}/media/${encodeURIComponent(photoId)}`, {expected: 404});
await request(`/posts/${postId}`, {method: 'DELETE', expected: 204});
await request(`/posts/${postId}/media/${encodeURIComponent(videoId)}`, {expected: 404});
console.log('PASS: authenticated photo/video upload, invalid file rejection, ordered attachments, byte range, replacement and deleted-post access denial.');
