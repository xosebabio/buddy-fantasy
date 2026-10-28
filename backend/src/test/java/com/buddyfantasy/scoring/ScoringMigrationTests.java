package com.buddyfantasy.scoring;

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

/** V12: scoring's copies of sheets and locked lineups, with cascades inside the module and no FK to other modules. */
@SpringBootTest
@Import(PostgresTestConfig.class)
class ScoringMigrationTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    UUID insertMatch() {
        UUID matchId = UUID.randomUUID();
        jdbc.update("insert into scoring_match (match_id, league_id, matchday, kickoff_at) values (?, ?, 1, ?)",
                matchId, UUID.randomUUID(), Timestamp.from(KICKOFF));
        return matchId;
    }

    void insertPlayerScore(UUID matchId, UUID playerId, String position, int goals, Integer actionPoints,
            Integer points) {
        jdbc.update("""
                insert into scoring_player_score (match_id, player_id, league_id, position, goals, penalty_goals,
                  penalties_saved, penalties_missed, assists, own_goals, yellow_card, double_yellow, red_card,
                  action_points, points)
                values (?, ?, ?, ?, ?, 0, 0, 0, 0, 0, false, false, false, ?, ?)""",
                matchId, playerId, UUID.randomUUID(), position, goals, actionPoints, points);
    }

    void insertLineup(UUID matchId, UUID userId) {
        jdbc.update("insert into scoring_lineup (match_id, user_id, league_id, over_budget) values (?, ?, ?, false)",
                matchId, userId, UUID.randomUUID());
    }

    void insertLineupPlayer(UUID matchId, UUID userId, UUID playerId) {
        jdbc.update("insert into scoring_lineup_player (match_id, user_id, player_id) values (?, ?, ?)",
                matchId, userId, playerId);
    }

    int count(String table, UUID matchId) {
        return jdbc.queryForObject("select count(*) from " + table + " where match_id = ?", Integer.class, matchId);
    }

    @Test
    @DisplayName("Deleting a match deletes its players, lineups and lineup players")
    void deletingAMatchDeletesItsPlayersLineupsAndLineupPlayers() {
        UUID match = insertMatch();
        UUID user = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        insertPlayerScore(match, player, "FORWARD", 1, 3, 3);
        insertLineup(match, user);
        insertLineupPlayer(match, user, player);

        jdbc.update("delete from scoring_match where match_id = ?", match);

        assertThat(count("scoring_player_score", match)).isZero();
        assertThat(count("scoring_lineup", match)).isZero();
        assertThat(count("scoring_lineup_player", match)).isZero();
    }

    @Test
    @DisplayName("Rows belong to an existing match and are unique per player and user")
    void rowsBelongToAnExistingMatchAndAreUniquePerPlayerAndUser() {
        UUID match = insertMatch();
        UUID user = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        assertThatThrownBy(() -> insertPlayerScore(UUID.randomUUID(), player, "FORWARD", 0, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertLineupPlayer(match, user, player))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertPlayerScore(match, player, "FORWARD", 0, null, null);
        insertLineup(match, user);
        insertLineupPlayer(match, user, player);
        assertThatThrownBy(() -> insertPlayerScore(match, player, "FORWARD", 0, null, null))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertLineup(match, user)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertLineupPlayer(match, user, player)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("The position is a valid position and the counts are non-negative")
    void positionIsAPositionAndCountsAreNonNegative() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> insertPlayerScore(match, UUID.randomUUID(), "COACH", 0, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPlayerScore(match, UUID.randomUUID(), "FORWARD", -1, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertPlayerScore(match, UUID.randomUUID(), "GOALKEEPER", 200, 1200, 1200))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Points are the action points plus the rating points, or null until scored")
    void pointsAreTheActionPointsPlusTheRatingPointsOrNullUntilScored() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> insertPlayerScore(match, UUID.randomUUID(), "FORWARD", 1, 3, 4))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPlayerScore(match, UUID.randomUUID(), "FORWARD", 1, 3, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPlayerScore(match, UUID.randomUUID(), "FORWARD", 1, null, 3))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertPlayerScore(match, UUID.randomUUID(), "FORWARD", 1, -3, -3))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A scored match has its frozen rules and both copies")
    void aScoredMatchHasItsFrozenRulesAndBothCopies() {
        UUID match = insertMatch();
        Timestamp at = Timestamp.from(KICKOFF);

        assertThatThrownBy(() -> jdbc.update("update scoring_match set scored_at = ? where match_id = ?", at, match))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "update scoring_match set rules = '{}'::jsonb, scored_at = ?, sheet_at = ? where match_id = ?",
                at, at, match)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> jdbc.update("""
                update scoring_match set rules = '{}'::jsonb, scored_at = ?, sheet_at = ?, lineups_at = ?
                where match_id = ?""", at, at, at, match)).doesNotThrowAnyException();
    }
}
