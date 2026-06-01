# ── 1단계: 빌드 ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

# Gradle에 Java 25 설치 위치를 직접 알려줘 auto-provisioning 방지
ENV GRADLE_OPTS="-Dorg.gradle.java.installations.auto-download=false \
                 -Dorg.gradle.java.installations.paths=/opt/java/openjdk"

# Gradle Wrapper + 의존성 파일만 먼저 복사해 레이어 캐시 활용
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# 소스 전체 복사 후 JAR 빌드 (테스트 제외)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test -q

# ── 2단계: 실행 이미지 ─────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

WORKDIR /app

# 보안: 전용 비root 사용자
RUN groupadd -r stocknews && useradd -r -g stocknews stocknews

COPY --from=builder /workspace/build/libs/stock-service-backend-*.jar app.jar

RUN chown stocknews:stocknews app.jar
USER stocknews

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
