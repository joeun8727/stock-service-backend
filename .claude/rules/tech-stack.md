# 00 — 기술 스택 & 코딩 컨벤션

## 확정 스택

| 항목 | 버전/선택                                              |
|------|----------------------------------------------------|
| Framework | Spring Boot 4.x                                    |
| Language | Java 25                                            |
| Build | Gradle (Kotlin DSL 권장: `build.gradle.kts`)         |
| DB | MySQL 8.x                                          |
| Cache | Redis 7.x                                          |
| ORM | Spring Data JPA + Hibernate                        |
| HTTP Client | Spring `RestClient` (WebClient 대신, 동기 호출 위주)       |
| Resilience | Resilience4j (Circuit Breaker, Retry, RateLimiter) |
| Migration | Flyway                                             |
| Test | JUnit 5 + Mockito + Testcontainers                 |
| 컨테이너 | Docker Compose (MySQL + Redis)                     |

> ⚠️ **Spring Boot 4.x + Java 25은 최신/Early Access 단계일 수 있음.** 라이브러리 호환성 문제 발생 시 즉시 보고하고, 안정 버전 대안(예: Spring Boot 3.x + Java 21)을 제안할 것.

## 주요 의존성 (build.gradle.kts)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
}
```

## 코딩 컨벤션

- **주석은 한국어**로 작성, 코드 식별자(클래스/메서드/변수)는 영어.
- **불변성 우선**: DTO는 `record`로, 엔티티 setter 최소화.
- **명시적 타입**: `var` 남용 금지, 반환 타입 명확히.
- **Lombok 사용**: `@Getter`, `@Builder`, `@RequiredArgsConstructor` (단, `@Data` 금지).
- **Null 회피**: `Optional` 반환, 컬렉션은 빈 컬렉션 반환 (null 금지).
- **상수 분리**: 매직 넘버 금지. Rate Limit 한도, TTL 등은 상수/설정값으로.
- **예외**: 커스텀 예외(`BusinessException` + `ErrorCode` enum) 사용, 전역 `@RestControllerAdvice`로 처리.

## 설정값 관리

- 모든 API 키는 **환경변수**로 주입. `application.yml`에 하드코딩 절대 금지.
- 환경변수 예: `FINNHUB_API_KEY`, `FRED_API_KEY`, `GEMINI_API_KEY`.
- 로컬 개발용은 `.env` 파일 + `.gitignore` 처리.

## 공통 응답 포맷

모든 API는 `ApiResponse<T>`로 감싸 반환:

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String disclaimer,   // 면책 문구 (compliance.md)
    ErrorInfo error,
    Instant timestamp
) {}
```
