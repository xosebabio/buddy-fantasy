package com.buddyfantasy.lineup;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** V11: lineup versions and match locks, with cascades inside the module and no FK to other modules. */
@SpringBootTest
@Import(PostgresTestConfig.class)
class LineupMigrationTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    UUID insertVersion(UUID... playerIds) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into lineup_version (id, league_id, user_id, saved_at) values (?, ?, ?, ?)",
                id, UUID.randomUUID(), UUID.randomUUID(), Timestamp.from(KICKOFF));
        for (UUID playerId : playerIds) {
            jdbc.update("insert into lineup_version_player (version_id, player_id) values (?, ?)", id, playerId);
        }
        return id;
    }

    UUID insertLock() {
        UUID matchId = UUID.randomUUID();
        jdbc.update("""
                insert into lineup_match_lock (match_id, league_id, matchday, kickoff_at, lock_at, locked_at)
                values (?, ?, 1, ?, ?, ?)""", matchId, UUID.randomUUID(), Timestamp.from(KICKOFF),
                Timestamp.from(KICKOFF), Timestamp.from(KICKOFF));
        return matchId;
    }

    void insertLockedLineup(UUID matchId, UUID userId, UUID versionId, BigDecimal totalValue, BigDecimal budget) {
        jdbc.update("""
                insert into lineup_locked_lineup (match_id, user_id, version_id, total_value, budget, over_budget)
                values (?, ?, ?, ?, ?, false)""", matchId, userId, versionId, totalValue, budget);
    }

    void insertLockedPlayer(UUID matchId, UUID userId, UUID playerId, BigDecimal value) {
        jdbc.update("insert into lineup_locked_player (match_id, user_id, player_id, value) values (?, ?, ?, ?)",
                matchId, userId, playerId, value);
    }

    int count(String table, UUID matchId) {
        return jdbc.queryForObject("select count(*) from " + table + " where match_id = ?", Integer.class, matchId);
    }

    @Test
    @DisplayName("A player appears once in a version and deleting the version deletes its players")
    void aPlayerAppearsOnceInAVersionAndDeletingTheVersionDeletesItsPlayers() {
        UUID player = UUID.randomUUID();
        UUID version = insertVersion(player);

        assertThatThrownBy(() -> jdbc.update(
                "insert into lineup_version_player (version_id, player_id) values (?, ?)", version, player))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("delete from lineup_version where id = ?", version);
        assertThat(jdbc.queryForObject("select count(*) from lineup_version_player where version_id = ?",
                Integer.class, version)).isZero();
    }

    @Test
    @DisplayName("Deleting a lock deletes its lineups and players but not the versions")
    void deletingALockDeletesItsLineupsAndPlayersButNotTheVersions() {
        UUID player = UUID.randomUUID();
        UUID version = insertVersion(player);
        UUID match = insertLock();
        UUID user = UUID.randomUUID();
        insertLockedLineup(match, user, version, BigDecimal.TEN, null);
        insertLockedPlayer(match, user, player, BigDecimal.TEN);

        jdbc.update("delete from lineup_match_lock where match_id = ?", match);

        assertThat(count("lineup_locked_lineup", match)).isZero();
        assertThat(count("lineup_locked_player", match)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from lineup_version where id = ?", Integer.class, version))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("A locked lineup needs an existing version and is unique per user")
    void aLockedLineupNeedsAnExistingVersionAndIsUniquePerUser() {
        UUID match = insertLock();
        UUID user = UUID.randomUUID();
        UUID version = insertVersion();

        assertThatThrownBy(() -> insertLockedLineup(match, user, UUID.randomUUID(), BigDecimal.ONE, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertLockedLineup(match, user, version, BigDecimal.ONE, null);
        assertThatThrownBy(() -> insertLockedLineup(match, user, version, BigDecimal.ONE, null))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("Locked players belong to a locked lineup")
    void lockedPlayersBelongToALockedLineup() {
        UUID match = insertLock();

        assertThatThrownBy(() -> insertLockedPlayer(match, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Values are non-negative and the budget is optional")
    void valuesAreNonNegativeAndTheBudgetIsOptional() {
        UUID match = insertLock();
        UUID version = insertVersion();
        UUID user = UUID.randomUUID();

        assertThatThrownBy(() -> insertLockedLineup(match, user, version, new BigDecimal("-1"), null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertLockedLineup(match, user, version, new BigDecimal("12.50"), null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> insertLockedPlayer(match, user, UUID.randomUUID(), new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
