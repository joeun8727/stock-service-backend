plugins {
    java
    id("org.springframework.boot") version "4.0.6"
}

group = "com.stocknews"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot BOM — Gradle 9.x 네이티브 방식 (io.spring.dependency-management 대체)
    // annotationProcessor/compileOnly는 implementation을 상속하지 않아 각각 명시 필요
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))

    // Spring Boot 핵심
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // MySQL 드라이버
    runtimeOnly("com.mysql:mysql-connector-j")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Java Time 직렬화 (Instant, LocalDate 등)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
