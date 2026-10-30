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

별도 터미널에서 아래 다섯 서비스를 실행한다. Gateway는 위 명령으로 실행해 둔다.

```powershell
.\gradlew.bat :user-service:bootRun
.\gradlew.bat :post-service:bootRun
.\gradlew.bat :comment-service:bootRun
.\gradlew.bat :follow-service:bootRun
.\gradlew.bat :facade:bootRun
```

- 화면의 `실제 API로 전환`을 누른 뒤 회원가입·로그인한다. 로그인 성공 시 실제 API 모드의 내 기록을 조회한다.
- 토큰은 sessionStorage에 보관하며 로그아웃하면 지운다. 페이지를 다시 열면 데모 모드로 시작한다.
- 실제 API 모드에서는 내 게시물 또는 검색란에 입력한 회원 키의 게시물을 조회한다. 전체 피드 API는 아직 없어 제공하지 않는다.
- 글 작성·수정·삭제와 댓글 작성·수정·삭제, 커서 기반 더 보기를 지원한다. 작성자 권한은 서버에서도 검증한다.
- 썸네일은 http(s) URL만 받으며 이미지 업로드·좋아요는 지원하지 않는다.
- 같은 Gateway 출처로 요청하므로 별도 CORS 설정이나 프런트엔드 빌드가 필요 없다.
- `/ui/**`의 GET만 익명에게 열고 기존 API 인증 정책은 유지한다. 프록시 경로는 `/api/**`로 제한했다.
- 기존 H2 메모리 DB 설정으로 각 서비스 재시작 시 실제 데이터가 사라진다. 운영용 화면/인증 구현으로 확정한 것은 아니다.

## 검증

```powershell
node --check apps/api-gateway/src/main/resources/static/ui/app.js
.\gradlew.bat :api-gateway:test
```

Gateway 통합 테스트에서 익명 정적 파일 제공과 API의 익명 접근 차단을 검증한다.
