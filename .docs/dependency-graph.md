# Dependency Graph

## 플러그인 계층

```
base-spring.core
  ├── base-spring.data      (+ spring-boot-starter-data-jpa)
  ├── base-spring.security  (+ spring-boot-starter-security, spring-security-test)
  └── base-spring.web       (+ spring-boot-starter-web, validation)
        └── base-spring.feign  (+ spring-cloud-starter-openfeign)
```

## 모듈 의존성

```
shared/api-contracts
  plugin : base-spring.core
  api    : spring-boot-starter-validation
           jackson-databind

  ※ HTTP DTO·헤더 계약만 공유. 엔티티·도메인 로직은 서비스 소유.

shared/common-infrastructure
  plugin : base-spring.core
  api    : spring-boot-starter
           jjwt-api / jjwt-impl / jjwt-jackson

apps/api-gateway
  plugin : base-spring.web
           base-spring.security  ← 게이트웨이만 Security 보유
           org.springframework.boot
  impl   : common-infrastructure
           api-contracts
  ※ DB 없음

apps/facade
  plugin : base-spring.feign
           org.springframework.boot
  impl   : api-contracts
  ※ DB 없음, Security 없음

services/member-service
  plugin : base-spring.data
           base-spring.web
           org.springframework.boot
  impl   : api-contracts
           common-infrastructure
  runtime: h2
  ※ Security 없음

services/post-service
  plugin : base-spring.data
           base-spring.web
           org.springframework.boot
  impl   : api-contracts
           common-infrastructure
  runtime: h2
  ※ Security 없음
```

## 전이 의존성 요약

`services/comment-service`는 post-service와 독립적인 Spring Boot/JPA 모듈이다.
`base-spring.data`, `base-spring.web`, `org.springframework.boot` 플러그인과
공통 DTO·인프라 모듈을 사용하며 H2 `commentdb`(8084)를 소유한다.
서비스 모듈 간 Gradle 의존성과 JPA 엔티티 공유는 없다.
Facade가 member-service(8081), post-service(8082), comment-service(8084)를 HTTP로 조정한다.
게시물 삭제 후 댓글 정리는 post-service의 DB 작업 처리기가 comment-service의 내부 DELETE API를 호출한다.
삭제 요청은 이 호출을 기다리지 않으며, 실패한 정리는 저장된 작업을 바탕으로 재시도한다.

| 모듈 | Spring Web | Spring Security | JPA | JWT |
|------|:---:|:---:|:---:|:---:|
| api-contracts | - | - | - | - |
| common-infrastructure | - | - | - | ✓ |
| api-gateway | ✓ | ✓ | - | ✓ |
| facade | ✓ | - | - | - |
| member-service | ✓ | - | ✓ | ✓ |
| post-service | ✓ | - | ✓ | - |
| comment-service | ✓ | - | ✓ | - |
| follow-service | ✓ | - | ✓ | - |
| media-service | ✓ | - | ✓ | - |

`services/follow-service`는 `base-spring.data`, `base-spring.web`, Boot 및 api-contracts을 사용한다.
독립 H2 followdb(8085)를 소유하며 다른 서비스 모듈 의존성이 없다. 회원 상태 조합은 Facade가 담당한다.

`services/media-service`는 `base-spring.data`, `base-spring.web`, Boot, api-contracts을 사용한다.
H2 mediadb(8086)와 로컬 파일 디렉터리를 소유한다. Facade가 업로드 회원 상태·첨부 소유권·게시글별 파일 접근을 조정한다.
