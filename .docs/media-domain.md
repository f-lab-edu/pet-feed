# 미디어 업로드와 게시글 첨부

작성일: 2026-11-05 (Asia/Seoul)

## 경계와 저장

- media-service(8086)는 `media_files` 메타데이터와 로컬 파일을 소유한다. 다른 서비스 엔티티나 저장소는 공유하지 않는다.
- 메타데이터: 서버 발급 UUID, 업로더 회원 키, 검증된 MIME, 바이트 수, 생성 시각(Instant).
- 파일명은 UUID만 사용한다. 사용자 파일명과 경로를 파일 시스템 경로로 사용하지 않는다.
- 기본 파일 경로는 media-service 실행 디렉터리의 `media-storage`이며 `MEDIA_STORAGE_PATH`로 변경한다. 객체 저장소 연동은 미구현이다.
- post-service의 `post_media(post_id, display_order, media_id)`는 게시글과 첨부 ID·순서만 저장한다. 미디어 DB 외래 키는 없다.

## 외부 API

1. `POST /api/v1/media`: JWT 필요. Content-Type에 `image/jpeg`, `image/png`, `video/mp4` 중 하나를 지정하고 파일 바이트를 그대로 전송한다. multipart가 아니다. 성공 201, 응답은 `{mediaId, contentType, size}`다.
2. `POST /api/v1/posts`, `PUT /api/v1/posts/{id}`: 기존 본문에 `mediaIds` 배열을 추가한다. 최대 6개이며 중복·잘못된 UUID는 거부한다. Facade는 미디어 서비스에 업로드 존재와 작성자 소유 여부를 확인한 뒤 저장을 전달한다.
3. 상세 응답의 `mediaIds`는 저장 순서를 유지한다. 수정 요청에서 필드 생략/null이면 기존 첨부 유지, 빈 배열이면 모두 제거, 값이 있으면 전체 교체다. 본문은 기존처럼 필수다.
4. `GET /api/v1/posts/{postId}/media/{mediaId}`: JWT 필요. Facade가 게시글이 조회 가능하고 해당 ID가 첨부되어 있는지 확인한 뒤 파일을 반환한다. 삭제된 게시글이나 첨부에서 제거된 ID는 404다. Range 요청과 206 응답을 전달한다.

미디어 내부 API는 `/internal/v1/media`, `/validate`, `/{id}/content`다. 기존 서비스와 마찬가지로 내부 네트워크의 인증/접근 통제는 별도 과제이며 공개 배포 시 직접 노출하면 안 된다.

## 검증과 실패

- JPEG·PNG: 10MiB 이하, 실제 이미지 형식과 선언 MIME 일치, 이미지 디코딩 가능 여부 및 4천만 픽셀 제한을 검사한다.
- MP4: 50MiB 이하, ftyp 브랜드 및 최상위 박스 길이, moov/mdat 존재를 확인한다. 전체 영상 디코딩·코덱 검사·트랜스코딩·썸네일 추출은 하지 않는다. 컨테이너 검사만으로 재생 가능성을 보장하지 않는다.
- 빈 파일·미지원/위장 형식은 415, 크기 초과는 413, 다른 회원의 미디어 연결은 403이다.
- 업로드의 파일 저장/DB 저장 실패 시 생성한 파일을 정리한다. 프로세스가 그 사이 강제 종료되는 경우의 고아 파일 정리는 별도 과제다.
- 파일은 불변이며 연결 해제/게시글 소프트 삭제로 즉시 지우지 않는다. 작성 취소·미연결 파일의 만료 정리와 영구 삭제 정책은 아직 없다.
- 현재 H2 메모리 DB는 재시작 시 초기화되지만 파일은 디스크에 남는다. 재시작 후 보존을 위해 영속 DB와 마이그레이션이 필요하다.

## 화면과 성능

- API 모드에서 파일 선택, 사진/영상 미리보기, 제거, 업로드 진행 상태, 실패 재시도를 지원한다. 성공한 업로드 ID는 같은 편집 창에서 재사용한다.
- 피드는 사용자가 첨부 보기 버튼을 누를 때 인증 fetch로 원본 전체를 가져온 뒤 Blob URL로 표시한다. 갱신/편집창 종료 시 URL을 해제한다. 큰 영상의 점진적 스트리밍과 CDN/서명 URL은 후속 작업이다.
- Gateway와 Facade는 현재 byte[]로 파일을 중계한다. 50MiB 제한은 요청당 제한이며, 동시 업로드의 메모리·대역폭 제한 및 사용자 저장량 제한은 후속 작업이다.
- 이미 내려받은 파일이나 삭제 직전에 시작된 조회까지 회수하지는 않는다. 신규 파일 조회에는 게시글 상태 검사를 적용하며 응답은 no-store다.

## 검증 실행

```powershell
.\gradlew.bat :media-service:test :post-service:test :comment-service:test :facade:test :api-gateway:test --console=plain
node --check apps/api-gateway/src/main/resources/static/ui/app.js
node scripts/media-smoke.mjs <로컬-MP4-경로>
```

마지막 명령은 로컬 7개 서비스 실행 후 테스트 계정과 첨부를 만들고 게시글을 삭제해 접근 차단까지 확인한다. 업로드 테스트 파일은 현재 보존 정책에 따라 디스크에 남는다.

참고: [Spring MVC Range 지원](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-range.html), [MDN 미디어 컨테이너와 코덱](https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Formats/Containers). 실제 HTTP 검증용 공개 MP4는 [MDN CC0 예제](https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4)를 사용했다.
