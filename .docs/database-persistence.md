# 로컬 DB 영속화

회원·게시글·댓글·팔로우·미디어 서비스는 각각 H2 파일 DB를 사용한다. 기본 위치는 **각 서비스 프로세스의 실행 디렉터리** 아래 `data/<서비스명>db.mv.db`다. Gradle `bootRun`은 일반적으로 해당 모듈 디렉터리에서 실행되므로 JAR를 다른 디렉터리에서 실행하면 다른 DB를 열 수 있다. 실행 위치에 관계없이 같은 DB를 사용하려면 절대 경로를 지정한다.

| 서비스 | 환경 변수 | 기본 JDBC URL |
| --- | --- | --- |
| 회원 | `MEMBER_DB_URL` (기존 `USER_DB_URL`도 지원) | `jdbc:h2:file:./data/userdb;DB_CLOSE_ON_EXIT=FALSE` |
| 게시글 | `POST_DB_URL` | `jdbc:h2:file:./data/postdb;DB_CLOSE_ON_EXIT=FALSE` |
| 댓글 | `COMMENT_DB_URL` | `jdbc:h2:file:./data/commentdb;DB_CLOSE_ON_EXIT=FALSE` |
| 팔로우 | `FOLLOW_DB_URL` | `jdbc:h2:file:./data/followdb;DB_CLOSE_ON_EXIT=FALSE` |
| 미디어 | `MEDIA_DB_URL` | `jdbc:h2:file:./data/mediadb;DB_CLOSE_ON_EXIT=FALSE` |

PowerShell 실행 예:

```powershell
$env:MEDIA_DB_URL = 'jdbc:h2:file:C:/pet-feed-data/media/mediadb;DB_CLOSE_ON_EXIT=FALSE'
$env:MEDIA_STORAGE_PATH = 'C:/pet-feed-data/media/files'
.\gradlew.bat :media-service:bootRun
```

미디어 DB와 실제 파일을 함께 보존해야 한다. DB 경로만 유지하고 파일 경로를 변경하면 기존 첨부 조회는 실패한다. DB 파일은 Git에 포함하지 않는다. 같은 파일 DB를 여러 서비스 인스턴스가 동시에 열지 않는다.

회원 모듈은 `services/member-service`로 변경했다. 기존 로컬 `data` 디렉터리는 모듈과 함께 이동했고, DB 파일명 `userdb`와 기존 `user_key` 컬럼은 그대로 사용한다. Java 필드 `memberKey`는 `@Column(name = "user_key")`로 매핑하므로 기존 V1 마이그레이션을 수정하지 않는다. 별도 환경의 절대 경로는 `MEMBER_DB_URL`로 기존 DB 파일을 명시한다.

## 스키마 변경

- 각 서비스의 `src/main/resources/db/migration/V1__initial_schema.sql`이 소유 테이블·인덱스·제약 조건을 생성한다.
- Flyway가 시작 시 버전과 체크섬을 검사하고 아직 적용되지 않은 마이그레이션을 실행한다. Hibernate는 `ddl-auto: validate`로 엔티티와 스키마를 검증한다. 종료 시 테이블을 삭제하지 않는다.
- 적용된 V1을 수정하지 않고 `V2__설명.sql`처럼 새 마이그레이션을 추가한다. `create`, `create-drop`, `update`로 스키마를 우회 변경하지 않는다.
- 기존 메모리 DB의 데이터는 자동 이전되지 않는다. 이전 프로세스가 종료되면 복구할 수 없다. 아직 실행 중인 메모리 DB에 보존할 데이터가 있다면 종료 전에 별도 내보내기·이관이 필요하다.
- 이미 테이블이 있는 임의의 DB를 자동 baseline하지 않는다. 기존 파일 DB를 연결하려면 현재 스키마와 이력을 확인한 뒤 이관 절차를 별도로 정한다.

## 테스트와 보존 범위

일반 테스트는 `src/test/resources/application.properties`의 고유한 메모리 DB를 사용하되, 실제 실행과 같은 Flyway 마이그레이션 및 Hibernate 검증을 거친다. 재시작 테스트는 JUnit 임시 폴더에 파일 DB를 생성하고 애플리케이션 컨텍스트와 커넥션 풀을 완전히 닫은 뒤 같은 DB로 다시 시작한다.

- 회원 식별자·탈퇴 상태, 팔로우 관계·중복 방지
- 댓글과 닫힌 댓글 스레드 보존 및 늦게 도착한 생성 요청 거부
- 미디어 메타데이터·소유권·원본 바이트·Range 조회
- 게시글 첨부 순서·삭제 상태·댓글 정리 실패 횟수 보존, 재시도 완료 및 완료 상태의 재시작 보존

```powershell
.\gradlew.bat :member-service:test :post-service:test :comment-service:test :follow-service:test :media-service:test --console=plain
```

이 단계는 단일 프로세스의 정상 종료·재시작을 검증한다. 별도 JVM 강제 종료나 전원 장애 복구, 운영 DB 전환, 다중 인스턴스, 백업 복원은 검증 범위 밖이다. 백업은 서비스를 정상 종료한 후 각 DB 파일과 미디어 파일을 함께 복사한다. PostgreSQL/MySQL 전환에는 해당 JDBC/Flyway 모듈과 SQL 호환성 검증이 추가로 필요하다.

참고: [Spring Boot 3.4 DB 초기화와 Flyway](https://docs.spring.io/spring-boot/3.4/how-to/data-initialization.html).
