package com.stocknews.api.support;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 모든 Repository 테스트의 공통 기반
// - DataJpaTest: JPA 슬라이스 (Entity + Repository + Flyway만 로드)
// - replace=NONE: Testcontainer MySQL 사용 (H2 대체 비활성)
// - FlywayAutoConfiguration: V1__init.sql 실행 → ddl-auto:validate와 조합해 스키마 일치 검증
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
public abstract class RepositoryTestSupport {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
}
