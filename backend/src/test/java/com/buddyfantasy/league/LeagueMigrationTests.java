package com.buddyfantasy.league;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
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
 * {@code league.limits}.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class LeagueMigrationTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    /** Inserts settings without the V9 columns, as rows written before the migration. */
    UUID insertLeagueWithSettings() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into league_league (id, name, created_at) values (?, 'Peña', ?)", id, Timestamp.from(NOW));
        jdbc.update("""
                insert into league_settings (league_id, rating_window_hours, rating_format, rating_points_per_unit,
                                             lineup_lock_hours, lineup_max_changes, lineup_size,
                                             lineup_formation_enabled, lineup_budget_enabled, lineup_budget,
                                             market_k, market_max_change_pct, market_min_value)
                values (?, 24, 'SPADES', 1, 2, 2, 7, false, true, 100, 0.05, 10, 1)""", id);
        return id;
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
}
