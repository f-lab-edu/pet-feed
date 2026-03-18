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
shared/common-domain
  plugin : base-spring.core
  api    : spring-boot-starter-validation
           jackson-databind

shared/common-infrastructure
  plugin : base-spring.core
  api    : spring-boot-starter
           jjwt-api / jjwt-impl / jjwt-jackson

apps/api-gateway
  plugin : base-spring.web
           base-spring.security  ← 게이트웨이만 Security 보유
           org.springframework.boot
  impl   : common-infrastructure
           common-domain
  ※ DB 없음

apps/facade
  plugin : base-spring.feign
           org.springframework.boot
  impl   : common-domain
           common-infrastructure
  ※ DB 없음, Security 없음

services/user-service
  plugin : base-spring.data
           base-spring.web
           org.springframework.boot
  impl   : common-domain
           common-infrastructure
  runtime: h2
  ※ Security 없음

services/post-service
  plugin : base-spring.data
           base-spring.web
           org.springframework.boot
  impl   : common-domain
           common-infrastructure
  runtime: h2
  ※ Security 없음
```

## 전이 의존성 요약

| 모듈 | Spring Web | Spring Security | JPA | JWT |
|------|:---:|:---:|:---:|:---:|
| common-domain | - | - | - | - |
| common-infrastructure | - | - | - | ✓ |
| api-gateway | ✓ | ✓ | - | ✓ |
| facade | ✓ | - | - | - |
| user-service | ✓ | - | ✓ | - |
| post-service | ✓ | - | ✓ | - |
