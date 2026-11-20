# API contracts

서비스 간 HTTP 요청·응답 DTO와 헤더 이름을 공유하는 모듈이다. 기존 `common-domain`에는 공통 엔티티나 도메인 로직이 없었으므로 실제 역할에 맞게 이름을 변경했다.

- Java 패키지: `com.petfeed.contracts`
- 엔티티, 저장소, 비즈니스 규칙은 각각의 서비스에 둔다.
- JWT 구현은 `common-infrastructure`, 프록시 오류 전달용 `DownstreamException`은 Gateway가 소유한다.
- `MemberStatus`는 member-service 도메인이 소유한다. 외부 작성자 응답에는 별도의 `AuthorStatus`를 사용하고 서비스 경계에서 명시적으로 변환한다.
- `MemberInfoResponse`는 회원 기본 정보(닉네임·프로필 이미지·소개)다. `MemberProfileResponse`는 Facade가 게시글·팔로우 통계를 조합한 외부 응답이다. 제공자와 소비자는 같은 계약 DTO를 사용한다.
- 현재 이 모듈은 제공자와 소비자가 함께 사용하는 Java 계약이다. 서비스별 독립 배포·버전 관리가 필요해지면 계약을 서비스 단위로 나누거나 OpenAPI 기반으로 생성하는 방식을 검토한다.

## 회원 API 명칭

Java와 JSON은 `Member`, `memberKey`, `members`를 사용한다. 외부·내부 회원 경로는 `/members`, 인증된 회원 식별 헤더는 `X-Member-Key`다. `/users`, JSON `userKey`·`users`, `X-User-Key`에 대한 호환 별칭은 제공하지 않는다. 기존 클라이언트는 새 계약으로 함께 전환해야 한다.

이 변경은 저장 데이터 이름 변경과 분리한다. 기존 `user_key` 컬럼과 `userdb` 파일은 데이터 보존을 위해 그대로 읽으며, 도메인·API에서는 `memberKey`를 사용한다.
