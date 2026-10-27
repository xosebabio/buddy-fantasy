package com.buddyfantasy;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The test database: a single Postgres started once per JVM and shared by every Spring context, so Flyway migrates it
 * once and the other contexts only validate. {@link DatabaseCleaner} empties it before each test, and
 * {@link TestFixtures}, imported here so every Spring test has it, fills it. {@link RecordedEvents} is imported here
 * for the same reason, and the data source is wrapped so that {@link SqlStatements} counts its statements.
 *
 * <p>Postgres runs in {@link #TIME_ZONE}, as the JVM of the tests ({@code argLine} in the {@code pom.xml}), so
 * that every instant must travel as such: {@link TimeZoneTests}.
 *
 * <p>The container is not a bean: Spring Boot stops container beans when their context closes, which would take the
 * database away from the contexts still cached. Ryuk removes it when the JVM exits.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({TestFixtures.class, RecordedEvents.class})
public class PostgresTestConfig {

    /** Outside UTC, as the JVM of the tests (RNF-05): an implicit conversion to the server's zone breaks them. */
    static final String TIME_ZONE = "Europe/Madrid";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("TZ", TIME_ZONE)
            .withCommand("postgres",
                    "-c", "max_connections=300", // a connection pool per cached context
                    "-c", "timezone=" + TIME_ZONE);

    static {
        POSTGRES.start();
    }

    /** Static: a post-processor must not need this configuration's instance. */
    @Bean
    static BeanPostProcessor sqlStatementsDataSourceProxy() {
        return SqlStatements.proxyDataSources();
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
