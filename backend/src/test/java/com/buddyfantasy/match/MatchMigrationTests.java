package com.buddyfantasy.match;

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

/**
 * V8: constraints of the match tables, unique matchday and kickoff per league, and an audit log without FK. V10
 * leaves business limits (opponent length, max stats, matchday minimum) to {@code match.limits}.
 */
@SpringBootTest
@Import(PostgresTestConfig.class)
class MatchMigrationTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    UUID insertMatch(UUID leagueId, int matchday, Instant kickoffAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into match_match (id, league_id, matchday, opponent, venue, kickoff_at, status, created_at)
                values (?, ?, ?, 'Rival', 'HOME', ?, 'SCHEDULED', ?)""",
                id, leagueId, matchday, Timestamp.from(kickoffAt), Timestamp.from(KICKOFF));
        return id;
    }

    UUID insertMatch() {
        return insertMatch(UUID.randomUUID(), 1, KICKOFF);
    }

    void insertPerformance(UUID matchId, boolean yellow, boolean doubleYellow, boolean red) {
        jdbc.update("""
                insert into match_player_performance (match_id, player_id, position, yellow_card, double_yellow, red_card)
                values (?, ?, 'DEFENDER', ?, ?, ?)""", matchId, UUID.randomUUID(), yellow, doubleYellow, red);
    }

    @Test
    @DisplayName("Matchday and kickoff are unique per league")
    void matchdayAndKickoffAreUniquePerLeague() {
        UUID league = UUID.randomUUID();
        insertMatch(league, 1, KICKOFF);

        assertThatThrownBy(() -> insertMatch(league, 1, KICKOFF.plusSeconds(3600)))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertMatch(league, 2, KICKOFF)).isInstanceOf(DuplicateKeyException.class);
        assertThatCode(() -> insertMatch(UUID.randomUUID(), 1, KICKOFF)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects an invalid matchday, venue and status")
    void rejectsInvalidMatchdayVenueAndStatus() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> insertMatch(UUID.randomUUID(), -1, KICKOFF))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertMatch(UUID.randomUUID(), 0, KICKOFF)).doesNotThrowAnyException();
        assertThatThrownBy(() -> jdbc.update("update match_match set venue = 'NEUTRAL' where id = ?", match))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update match_match set status = 'CANCELLED' where id = ?", match))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("The result has both goals or none")
    void resultHasBothGoalsOrNone() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> jdbc.update("update match_match set goals_for = 1 where id = ?", match))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "update match_match set goals_for = -1, goals_against = 0 where id = ?", match))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> jdbc.update("update match_match set goals_for = 2, goals_against = 1 where id = ?",
                match)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("closed_at is set if and only if the match is closed")
    void closedAtIsSetIfAndOnlyIfTheMatchIsClosed() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> jdbc.update("update match_match set status = 'CLOSED' where id = ?", match))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update match_match set closed_at = ? where id = ?",
                Timestamp.from(KICKOFF), match)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> jdbc.update("update match_match set status = 'CLOSED', closed_at = ? where id = ?",
                Timestamp.from(KICKOFF), match)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A double yellow excludes a yellow and a straight red")
    void doubleYellowExcludesYellowAndStraightRed() {
        UUID match = insertMatch();

        assertThatThrownBy(() -> insertPerformance(match, true, true, false))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertPerformance(match, false, true, true))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertPerformance(match, false, true, false)).doesNotThrowAnyException();
        assertThatCode(() -> insertPerformance(match, true, false, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects negative counters and unknown positions")
    void rejectsNegativeCountersAndUnknownPositions() {
        UUID match = insertMatch();

        for (String counter : new String[] {"goals", "penalty_goals", "penalties_saved", "penalties_missed",
                "assists", "own_goals"}) {
            assertThatThrownBy(() -> jdbc.update("insert into match_player_performance (match_id, player_id, "
                    + "position, " + counter + ") values (?, ?, 'FORWARD', -1)", match, UUID.randomUUID()))
                    .as(counter).isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatCode(() -> jdbc.update("""
                insert into match_player_performance (match_id, player_id, position, goals)
                values (?, ?, 'FORWARD', 100)""", match, UUID.randomUUID())).doesNotThrowAnyException();
        assertThatThrownBy(() -> jdbc.update("""
                insert into match_player_performance (match_id, player_id, position, own_goals)
                values (?, ?, 'FORWARD', -1)""", match, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into match_player_performance (match_id, player_id, position)
                values (?, ?, 'COACH')""", match, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deleting a match deletes its performances but keeps the audit log")
    void deletingAMatchDeletesItsPerformancesButKeepsTheAuditLog() {
        UUID match = insertMatch();
        insertPerformance(match, false, false, false);
        jdbc.update("""
                insert into match_audit_entry (id, league_id, match_id, actor_user_id, action, occurred_at, after)
                values (?, ?, ?, ?, 'SCHEDULED', ?, '{}'::jsonb)""",
                UUID.randomUUID(), UUID.randomUUID(), match, UUID.randomUUID(), Timestamp.from(KICKOFF));

        jdbc.update("delete from match_match where id = ?", match);

        assertThat(jdbc.queryForObject("select count(*) from match_player_performance where match_id = ?",
                Integer.class, match)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from match_audit_entry where match_id = ?",
                Integer.class, match)).isOne();
    }

    @Test
    @DisplayName("The opponent is not limited by the column")
    void opponentIsNotLimitedByTheColumn() {
        UUID match = insertMatch();

        assertThatCode(() -> jdbc.update("update match_match set opponent = ? where id = ?", "x".repeat(500), match))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("The audit action must be known")
    void auditActionMustBeKnown() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into match_audit_entry (id, league_id, match_id, actor_user_id, action, occurred_at)
                values (?, ?, ?, ?, 'REOPENED', ?)""",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Timestamp.from(KICKOFF)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
