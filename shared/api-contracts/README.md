# API contracts

서비스 간 HTTP 요청·응답 DTO와 헤더 이름을 공유하는 모듈이다. 기존 `common-domain`에는 공통 엔티티나 도메인 로직이 없었으므로 실제 역할에 맞게 이름을 변경했다.

- Java 패키지: `com.petfeed.contracts`
- 엔티티, 저장소, 비즈니스 규칙은 각각의 서비스에 둔다.
- JWT 구현은 `common-infrastructure`, 프록시 오류 전달용 `DownstreamException`은 Gateway가 소유한다.
- `MemberStatus`는 응답 계약의 상태 값이다. 회원 상태 전환 규칙은 member-service에 둔다.
- 현재 이 모듈은 제공자와 소비자가 함께 사용하는 Java 계약이다. 서비스별 독립 배포·버전 관리가 필요해지면 계약을 서비스 단위로 나누거나 OpenAPI 기반으로 생성하는 방식을 검토한다.

## 회원 명칭과 호환성

Java 클래스·필드·메서드는 `Member`, `memberKey`, `members`를 사용한다. 기존 클라이언트와의 호환성을 위해 JSON은 `@JsonProperty`로 `userKey`, `users`를 유지하고 HTTP 경로 `/users`와 헤더 `X-User-Key`도 유지한다. Java 상수명은 `X_MEMBER_KEY`다. 외부 계약 변경은 별도 버전 변경으로 다룬다.
