package com.buddyfantasy.squad;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V7: constraints of {@code squad_player}, including the partial unique index on active shirt numbers. V10 leaves
 * business limits (name length, shirt number range) to {@code squad.limits}.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class SquadMigrationTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    UUID insert(UUID leagueId, Integer shirtNumber) {
        return insert(leagueId, shirtNumber, "FORWARD", "10.00");
    }

    UUID insert(UUID leagueId, Integer shirtNumber, String position, String initialValue) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into squad_player (id, league_id, name, shirt_number, position, initial_value, created_at)
                values (?, ?, 'Player', ?, ?, ?::numeric, ?)""",
                id, leagueId, shirtNumber, position, initialValue, Timestamp.from(NOW));
        return id;
    }

    void retire(UUID playerId) {
        jdbc.update("update squad_player set retired_at = ? where id = ?", Timestamp.from(NOW), playerId);
    }

    @Test
    void shirtNumberIsUniqueAmongActivePlayersOfALeague() {
        UUID league = UUID.randomUUID();
        insert(league, 9);

        assertThatThrownBy(() -> insert(league, 9)).isInstanceOf(DuplicateKeyException.class);
        assertThatCode(() -> insert(UUID.randomUUID(), 9)).doesNotThrowAnyException();
    }

    @Test
    void retiringAPlayerFreesTheShirtNumber() {
        UUID league = UUID.randomUUID();
        UUID retired = insert(league, 10);
        retire(retired);

        assertThatCode(() -> insert(league, 10)).doesNotThrowAnyException();
    }

    @Test
    void severalPlayersCanHaveNoShirtNumber() {
        UUID league = UUID.randomUUID();
        insert(league, null);

        assertThatCode(() -> insert(league, null)).doesNotThrowAnyException();
    }

    @Test
    void nameIsNotLimitedByTheColumn() {
        UUID player = insert(UUID.randomUUID(), null);

        assertThatCode(() -> jdbc.update("update squad_player set name = ? where id = ?", "x".repeat(500), player))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidShirtNumberPositionAndValue() {
        UUID league = UUID.randomUUID();

        assertThatThrownBy(() -> insert(league, -1)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insert(league, 100)).doesNotThrowAnyException();
        assertThatThrownBy(() -> insert(league, 1, "COACH", "10.00"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insert(league, 2, "FORWARD", "0"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
