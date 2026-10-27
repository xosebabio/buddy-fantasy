package com.buddyfantasy;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The test database: a single Postgres started once per JVM and shared by every Spring context, so Flyway migrates it
 * once and the other contexts only validate. {@link DatabaseCleaner} empties it before each test, and
 * {@link TestFixtures}, imported here so every Spring test has it, fills it.
 *
 * <p>The container is not a bean: Spring Boot stops container beans when their context closes, which would take the
 * database away from the contexts still cached. Ryuk removes it when the JVM exits.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(TestFixtures.class)
public class PostgresTestConfig {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCommand("postgres", "-c", "max_connections=300"); // a connection pool per cached context

    static {
        POSTGRES.start();
    }

    @Bean
    JdbcConnectionDetails postgresConnectionDetails() {
        return new JdbcConnectionDetails() {

            @Override
            public String getUsername() {
                return POSTGRES.getUsername();
            }

            @Override
            public String getPassword() {
                return POSTGRES.getPassword();
            }

            @Override
            public String getJdbcUrl() {
                return POSTGRES.getJdbcUrl();
            }
        };
    }
}
