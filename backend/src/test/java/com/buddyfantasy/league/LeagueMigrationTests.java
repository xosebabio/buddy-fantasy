package com.buddyfantasy.league;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.buddyfantasy.PostgresTestConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V9: per-position lineup minimums default to 1. V10 leaves business limits (name length, maximum lock hours) to
 * {@code league.limits}. V13: rating conversion per format and minimum of votes, backfilled with the defaults.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class LeagueMigrationTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    /** Inserts settings without the V9 columns, as rows written before that migration. */
    UUID insertLeagueWithSettings() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into league_league (id, name, created_at) values (?, 'Peña', ?)", id, Timestamp.from(NOW));
        jdbc.update("""
                insert into league_settings (league_id, rating_window_hours, rating_format, rating_spades_factor,
                                             rating_spades_base, rating_decimal_factor, rating_decimal_base,
                                             rating_min_votes, lineup_lock_hours, lineup_max_changes, lineup_size,
                                             lineup_formation_enabled, lineup_budget_enabled, lineup_budget,
                                             market_k, market_max_change_pct, market_min_value)
                values (?, 24, 'SPADES', 4, -2, 1, 0, 1, 2, 2, 7, false, true, 100, 0.05, 10, 1)""", id);
        return id;
    }

    /** Flyway on its own schema, so a test can stop at a version, write rows and apply the next migrations. */
    private Flyway flyway(String schema, String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).target(target).load();
    }

    @Test
    @DisplayName("The minimums per position default to one")
    void minimumsPerPositionDefaultToOne() {
        UUID league = insertLeagueWithSettings();

        Map<String, Object> row = jdbc.queryForMap("""
                select lineup_min_goalkeepers, lineup_min_defenders, lineup_min_midfielders, lineup_min_forwards
                from league_settings where league_id = ?""", league);

        assertThat(row.values()).hasSize(4).allSatisfy(value -> assertThat(((Number) value).intValue()).isOne());
    }

    @Test
    @DisplayName("Rejects negative minimums")
    void rejectsNegativeMinimums() {
        UUID league = insertLeagueWithSettings();

        for (String column : new String[] {"lineup_min_goalkeepers", "lineup_min_defenders",
                "lineup_min_midfielders", "lineup_min_forwards"}) {
            assertThatThrownBy(() -> jdbc.update(
                    "update league_settings set " + column + " = -1 where league_id = ?", league))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatCode(() -> jdbc.update("update league_settings set lineup_min_forwards = 0 where league_id = ?",
                league)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The lock hours can only be non-negative")
    void lockHoursAreOnlyNonNegative() {
        UUID league = insertLeagueWithSettings();

        assertThatCode(() -> jdbc.update("update league_settings set lineup_lock_hours = 169 where league_id = ?",
                league)).doesNotThrowAnyException();
        assertThatThrownBy(() -> jdbc.update("update league_settings set lineup_lock_hours = -1 where league_id = ?",
                league)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("The name is not limited by the column")
    void nameIsNotLimitedByTheColumn() {
        UUID league = insertLeagueWithSettings();

        assertThatCode(() -> jdbc.update("update league_league set name = ? where id = ?", "x".repeat(500), league))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("V13 backfills the existing leagues with the default rating conversion and minimum of votes")
    void v13BackfillsTheExistingLeaguesWithTheDefaultRatingConversion() {
        String schema = "league_migration_v13";
        try {
            flyway(schema, "12").migrate();
            UUID id = UUID.randomUUID();
            jdbc.update("insert into " + schema + ".league_league (id, name, created_at) values (?, 'Peña', ?)",
                    id, Timestamp.from(NOW));
            jdbc.update("insert into " + schema + """
                    .league_settings (league_id, rating_window_hours, rating_format, rating_points_per_unit,
                                      lineup_lock_hours, lineup_max_changes, lineup_size, lineup_formation_enabled,
                                      lineup_budget_enabled, lineup_budget, market_k, market_max_change_pct,
                                      market_min_value)
                    values (?, 24, 'DECIMAL', 1, 2, 2, 7, false, true, 100, 0.05, 10, 1)""", id);

            flyway(schema, "13").migrate();

            Map<String, Object> row = jdbc.queryForMap("""
                    select rating_format, rating_spades_factor, rating_spades_base, rating_decimal_factor,
                           rating_decimal_base, rating_min_votes
                    from %s.league_settings where league_id = ?""".formatted(schema), id);
            assertThat(row.get("rating_format")).isEqualTo("DECIMAL");
            assertThat(number(row, "rating_spades_factor")).isEqualTo(4);
            assertThat(number(row, "rating_spades_base")).isEqualTo(-2);
            assertThat(number(row, "rating_decimal_factor")).isEqualTo(1);
            assertThat(number(row, "rating_decimal_base")).isZero();
            assertThat(number(row, "rating_min_votes")).isEqualTo(1);
            assertThat(jdbc.queryForList("""
                    select column_name from information_schema.columns
                    where table_schema = ? and table_name = 'league_settings' and column_name like 'rating%'""",
                    String.class, schema)).containsExactlyInAnyOrder("rating_window_hours", "rating_format",
                    "rating_spades_factor", "rating_spades_base", "rating_decimal_factor", "rating_decimal_base",
                    "rating_min_votes");
            assertThat(jdbc.queryForList("""
                    select column_name from information_schema.columns
                    where table_schema = ? and table_name = 'league_settings'
                      and column_name like 'rating%' and column_default is not null""", String.class, schema))
                    .as("the defaults only backfill existing leagues").isEmpty();
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }

    @Test
    @DisplayName("Rejects a non-positive conversion factor or a minimum of votes below one")
    void rejectsNonPositiveConversionFactorOrMinimumOfVotesBelowOne() {
        UUID league = insertLeagueWithSettings();

        for (String assignment : new String[] {"rating_spades_factor = 0", "rating_decimal_factor = -1",
                "rating_min_votes = 0", "rating_spades_base = null"}) {
            assertThatThrownBy(() -> jdbc.update(
                    "update league_settings set " + assignment + " where league_id = ?", league))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatCode(() -> jdbc.update("""
                update league_settings set rating_spades_base = -5.5, rating_decimal_base = 3, rating_min_votes = 4
                where league_id = ?""", league)).doesNotThrowAnyException();
    }

    private static int number(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).intValue();
    }
}
