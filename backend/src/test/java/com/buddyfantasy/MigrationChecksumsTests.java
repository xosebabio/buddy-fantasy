package com.buddyfantasy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An applied migration is never edited: the test database is born empty, so Flyway never sees the old checksum and
 * would not notice. This compares the checksum of every migration Flyway applied with {@value #CHECKSUMS}, so
 * editing an applied migration fails here. A new migration is added to that file in the same change; the failure
 * message prints its line.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class MigrationChecksumsTests {

    static final String CHECKSUMS = "db/migration-checksums.txt";

    @Autowired
    JdbcTemplate jdbc;

    /** A line of {@value #CHECKSUMS}: {@code <version> <checksum> <script>}. */
    record Migration(String version, Integer checksum, String script) {

        static Migration parse(String line) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length != 3) {
                throw new IllegalArgumentException("Expected '<version> <checksum> <script>' in " + CHECKSUMS
                        + ", got: " + line);
            }
            return new Migration(fields[0], Integer.valueOf(fields[1]), fields[2]);
        }

        @Override
        public String toString() {
            return version + " " + checksum + " " + script;
        }
    }

    private static Map<String, Migration> recorded() throws IOException {
        String content = new ClassPathResource(CHECKSUMS).getContentAsString(StandardCharsets.UTF_8);
        Map<String, Migration> migrations = new LinkedHashMap<>();
        content.lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(Migration::parse)
                .forEach(migration -> migrations.put(migration.version(), migration));
        return migrations;
    }

    private Map<String, Migration> applied() {
        Map<String, Migration> migrations = new LinkedHashMap<>();
        jdbc.query("""
                select version, checksum, script from flyway_schema_history
                where success and version is not null order by installed_rank""",
                (rs, row) -> new Migration(rs.getString("version"), (Integer) rs.getObject("checksum"),
                        rs.getString("script")))
                .forEach(migration -> migrations.put(migration.version(), migration));
        return migrations;
    }

    @Test
    @DisplayName("Flyway applied the migrations")
    void flywayAppliedMigrations() {
        assertThat(applied()).isNotEmpty();
    }

    @Test
    @DisplayName("No applied migration was edited")
    void noAppliedMigrationWasEdited() throws IOException {
        Map<String, Migration> recorded = recorded();
        Map<String, Migration> applied = applied();

        List<String> edited = recorded.values().stream()
                .filter(migration -> applied.containsKey(migration.version())
                        && !Objects.equals(applied.get(migration.version()), migration))
                .map(migration -> migration + " is now " + applied.get(migration.version()))
                .toList();
        List<Migration> missing = recorded.values().stream()
                .filter(migration -> !applied.containsKey(migration.version()))
                .toList();
        List<Migration> added = applied.values().stream()
                .filter(migration -> !recorded.containsKey(migration.version()))
                .toList();

        assertThat(edited)
                .as("applied migrations changed: never edit one, revert it and add a new migration")
                .isEmpty();
        assertThat(missing)
                .as("applied migrations removed or renamed: never remove one, restore it")
                .isEmpty();
        assertThat(added)
                .as("new migrations: add these lines to src/test/resources/" + CHECKSUMS)
                .isEmpty();
    }
}
