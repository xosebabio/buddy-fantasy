package com.buddyfantasy;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Empties the shared test database ({@link PostgresTestConfig}) before each test method of every Spring test, so no
 * test sees the data of another one. Registered for all of them in {@code META-INF/spring.factories}.
 *
 * <p>First it waits for the event listeners still running ({@link Listeners#await}), or they would write into the
 * next test. Then it truncates every table of the schema with rows, read from {@code information_schema}, so the
 * tables of a new module are cleaned without listing them, {@code event_publication} and {@code spring_session}
 * included. Two are kept: Flyway's history, and {@code shedlock}, whose locks are released instead: ShedLock remembers
 * the rows it has created and only updates them afterwards, so a deleted row would keep its job from ever locking
 * again.
 */
public class DatabaseCleaner extends AbstractTestExecutionListener {

    /** Finds the tables with rows, since truncating the empty ones too is several times slower. */
    private static volatile String nonEmptyTables;

    @Override
    public int getOrder() {
        return 3500; // after dependency injection and @DirtiesContext, before a test transaction would start
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        JdbcTemplate jdbc = testContext.getApplicationContext().getBeanProvider(JdbcTemplate.class).getIfUnique();
        if (jdbc == null) {
            return;
        }
        try {
            Listeners.await(jdbc);
        } finally {
            // also after a listener of a previous test failed, to start clean
            List<String> tables = jdbc.queryForList(nonEmptyTables(jdbc), String.class);
            if (!tables.isEmpty()) {
                jdbc.execute("truncate table " + String.join(", ", tables) + " cascade");
            }
            jdbc.update("update shedlock set lock_until = locked_at");
        }
    }

    /** The tables are the same for every context, since they all share one database. */
    private static String nonEmptyTables(JdbcTemplate jdbc) {
        if (nonEmptyTables == null) {
            nonEmptyTables = jdbc.queryForList("""
                            select quote_ident(table_name) from information_schema.tables
                            where table_schema = current_schema() and table_type = 'BASE TABLE'
                              and table_name not in ('flyway_schema_history', 'shedlock')""", String.class)
                    .stream()
                    .map(table -> "select '" + table + "' where exists (select from " + table + ")")
                    .collect(Collectors.joining(" union all "));
        }
        return nonEmptyTables;
    }
}
