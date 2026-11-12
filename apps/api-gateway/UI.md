# 임시 화면

프로젝트 루트에서 Gateway를 실행하고 `http://localhost:8080/ui/index.html`을 연다.

```powershell
.\gradlew.bat :api-gateway:bootRun
```

## 데모 모드

- 기본 모드이며 다른 서비스를 실행할 필요가 없다.
- 예시 게시물 3개와 댓글 2개를 제공한다. `모카와 나`로 게시물·댓글을 작성하고 본인 기록을 수정·삭제할 수 있다.
- 데이터는 브라우저 localStorage의 `petfeed-demo-v1`에 저장된다. 해당 키를 지우면 예시 데이터로 돌아간다.
- 데모 기록은 실제 API에 전송되지 않는다.

## 실제 API 모드

별도 터미널에서 아래 여섯 서비스를 실행한다. Gateway는 위 명령으로 실행해 둔다.

```powershell
.\gradlew.bat :user-service:bootRun
.\gradlew.bat :post-service:bootRun
.\gradlew.bat :comment-service:bootRun
.\gradlew.bat :follow-service:bootRun
.\gradlew.bat :media-service:bootRun
.\gradlew.bat :facade:bootRun
```

- 화면의 `실제 API로 전환`을 누른 뒤 회원가입·로그인한다. 로그인 성공 시 실제 API 모드의 내 기록을 조회한다.
- 토큰은 sessionStorage에 보관하며 로그아웃하면 지운다. 페이지를 다시 열면 데모 모드로 시작한다.
- 실제 API 모드에서는 내 게시물 또는 검색란에 입력한 회원 키의 게시물을 조회한다. 전체 피드 API는 아직 없어 제공하지 않는다.
- 글 작성·수정·삭제와 댓글 작성·수정·삭제, 커서 기반 더 보기를 지원한다. 작성자 권한은 서버에서도 검증한다.
- 사진·동영상은 작성/수정 창에서 파일을 선택한다. 최대 6개, JPEG·PNG 사진 10MiB, MP4 동영상 50MiB까지 지원한다. 선택한 파일의 미리보기와 제거, 순차 업로드 상태를 표시한다.
- 저장 실패 후 같은 창에서 다시 저장하면 업로드가 끝난 파일 ID는 재사용한다. 게시글 수정 시 기존 첨부를 유지하거나 제거하고 새 파일을 추가할 수 있다.
- 피드의 `사진·동영상 N 보기`를 누르면 인증 헤더를 사용해 파일을 가져와 표시한다. 영상은 기본 재생 컨트롤을 제공하며 자동 재생하지 않는다. 현재 원본 전체를 받아 Blob URL로 표시하므로 대용량 스트리밍 최적화는 후속 작업이다.
- 기존 http(s) 썸네일 URL 입력도 유지한다. 좋아요는 지원하지 않는다. 동영상 변환·압축은 없으며 브라우저에서 재생 가능한 MP4 코덱이 필요하다.
- 업로드는 실제 API 모드에서만 제공한다. 데모 모드에서는 파일 선택이 비활성화된다.
- 같은 Gateway 출처로 요청하므로 별도 CORS 설정이나 프런트엔드 빌드가 필요 없다.
- `/ui/**`의 GET만 익명에게 열고 기존 API 인증 정책은 유지한다. 프록시 경로는 `/api/**`로 제한했다.
- 기존 H2 메모리 DB 설정으로 각 서비스 재시작 시 실제 데이터가 사라진다. 운영용 화면/인증 구현으로 확정한 것은 아니다.
- media-service는 로컬 `media-storage`에 파일을 저장한다. `MEDIA_STORAGE_PATH`로 변경 가능하다. DB 재시작 후 남은 파일 및 작성 취소 파일의 자동 정리는 아직 없으며, 자세한 정책은 [미디어 명세](../../.docs/media-domain.md)를 따른다.

## 검증

```powershell
node --check apps/api-gateway/src/main/resources/static/ui/app.js
.\gradlew.bat :api-gateway:test
```

Gateway 통합 테스트에서 익명 정적 파일 제공과 API의 익명 접근 차단을 검증한다.
