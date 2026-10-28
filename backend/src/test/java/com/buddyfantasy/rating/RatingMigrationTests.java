package com.buddyfantasy.rating;

import java.sql.Timestamp;
import java.time.Duration;
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

/** V14: rating windows, rateable players, votes and exclusions, with cascades inside the module. */
@SpringBootTest
@Import(PostgresTestConfig.class)
class RatingMigrationTests {

    static final Instant OPENED = Instant.parse("2026-01-11T13:00:00Z");
    static final Instant CLOSES = OPENED.plus(Duration.ofHours(24));

    @Autowired
    JdbcTemplate jdbc;

    UUID insertWindow() {
        UUID matchId = UUID.randomUUID();
        insertWindow(matchId, "SPADES", "4", 1, null, CLOSES);
        return matchId;
    }

    void insertWindow(UUID matchId, String format, String factor, int minVotes, String defaultRating,
            Instant closesAt) {
        jdbc.update("""
                insert into rating_window (match_id, league_id, matchday, kickoff_at, opened_at, closes_at, format,
                  conversion_factor, conversion_base, min_votes, default_rating, sheet_at)
                values (?, ?, 1, ?, ?, ?, ?, ?::numeric, -2, ?, ?::numeric, ?)""",
                matchId, UUID.randomUUID(), Timestamp.from(OPENED.minus(Duration.ofHours(3))),
                Timestamp.from(OPENED), Timestamp.from(closesAt), format, factor, minVotes, defaultRating,
                Timestamp.from(OPENED));
    }

    void insertPlayer(UUID matchId, UUID playerId, String position) {
        jdbc.update("insert into rating_player (match_id, player_id, position) values (?, ?, ?)",
                matchId, playerId, position);
    }

    void setResult(UUID matchId, UUID playerId, Integer votes, String average, Integer points) {
        jdbc.update("""
                update rating_player set votes = ?, average = ?::numeric, rating_points = ?
                where match_id = ? and player_id = ?""", votes, average, points, matchId, playerId);
    }

    void insertVote(UUID matchId, UUID playerId, UUID userId, int value) {
        jdbc.update("insert into rating_vote (match_id, player_id, user_id, value, updated_at) values (?, ?, ?, ?, ?)",
                matchId, playerId, userId, value, Timestamp.from(OPENED));
    }

    void insertExclusion(UUID matchId, UUID userId) {
        jdbc.update("""
                insert into rating_exclusion (match_id, user_id, excluded_by, excluded_at)
                values (?, ?, ?, ?)""", matchId, userId, UUID.randomUUID(), Timestamp.from(OPENED));
    }

    int count(String table, UUID matchId) {
        return jdbc.queryForObject("select count(*) from " + table + " where match_id = ?", Integer.class, matchId);
    }

    @Test
    @DisplayName("Removing a player from the sheet deletes his votes, and deleting a window deletes everything")
    void removingAPlayerDeletesHisVotesAndDeletingAWindowDeletesEverything() {
        UUID match = insertWindow();
        UUID keeper = UUID.randomUUID();
        UUID striker = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        insertPlayer(match, keeper, "GOALKEEPER");
        insertPlayer(match, striker, "FORWARD");
        insertVote(match, keeper, user, 3);
        insertVote(match, striker, user, 4);
        insertExclusion(match, UUID.randomUUID());

        jdbc.update("delete from rating_player where match_id = ? and player_id = ?", match, striker);

        assertThat(jdbc.queryForList("select player_id from rating_vote where match_id = ?", UUID.class, match))
                .containsExactly(keeper);

        jdbc.update("delete from rating_window where match_id = ?", match);

        assertThat(count("rating_player", match)).isZero();
        assertThat(count("rating_vote", match)).isZero();
        assertThat(count("rating_exclusion", match)).isZero();
    }

    @Test
    @DisplayName("Votes are of a player of the window, one per user and non-negative; exclusions one per user")
    void votesAreOfAPlayerOfTheWindowOnePerUserAndNonNegative() {
        UUID match = insertWindow();
        UUID player = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        assertThatThrownBy(() -> insertPlayer(UUID.randomUUID(), player, "FORWARD"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertVote(match, player, user, 3))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertExclusion(UUID.randomUUID(), user))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertPlayer(match, player, "FORWARD");
        insertVote(match, player, user, 3);
        insertExclusion(match, user);
        assertThatThrownBy(() -> insertPlayer(match, player, "FORWARD")).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertVote(match, player, user, 2)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertExclusion(match, user)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> insertVote(match, player, UUID.randomUUID(), -1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertVote(match, player, UUID.randomUUID(), 10)).doesNotThrowAnyException();
        assertThatThrownBy(() -> insertPlayer(match, UUID.randomUUID(), "COACH"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Frozen parameters: a valid format, a positive factor, at least 1 vote and no upper limits")
    void theFrozenParametersAreValidated() {
        assertThatThrownBy(() -> insertWindow(UUID.randomUUID(), "STARS", "4", 1, null, CLOSES))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertWindow(UUID.randomUUID(), "SPADES", "0", 1, null, CLOSES))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertWindow(UUID.randomUUID(), "SPADES", "4", 0, null, CLOSES))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertWindow(UUID.randomUUID(), "SPADES", "4", 1, "-1", CLOSES))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertWindow(UUID.randomUUID(), "SPADES", "4", 1, null, OPENED))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> insertWindow(UUID.randomUUID(), "DECIMAL", "9999.99", 500, "10", CLOSES))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A window is closed from closes_at, and a player's result has an average only with votes")
    void aWindowIsClosedFromClosesAtAndAResultHasAnAverageOnlyWithVotes() {
        UUID match = insertWindow();
        UUID player = UUID.randomUUID();
        insertPlayer(match, player, "FORWARD");

        assertThatThrownBy(() -> jdbc.update("update rating_window set closed_at = ? where match_id = ?",
                Timestamp.from(CLOSES.minusSeconds(1)), match)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> jdbc.update("update rating_window set closed_at = ? where match_id = ?",
                Timestamp.from(CLOSES), match)).doesNotThrowAnyException();
        assertThatThrownBy(() -> setResult(match, player, 2, null, 6))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> setResult(match, player, 0, "0", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> setResult(match, player, 2, "2", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> setResult(match, player, 0, null, 0)).doesNotThrowAnyException();
        assertThatCode(() -> setResult(match, player, 2, "2.50", 8)).doesNotThrowAnyException();
    }
}
