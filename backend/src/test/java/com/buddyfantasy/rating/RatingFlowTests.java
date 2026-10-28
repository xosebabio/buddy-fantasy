package com.buddyfantasy.rating;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.Listeners;
import com.buddyfantasy.MutableClock;
import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.RecordedEvents;
import com.buddyfantasy.TableDigests;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.rating.adapter.in.scheduler.CloseDueRatingWindowsJob;
import com.buddyfantasy.rating.application.usecases.applycorrectedsheet.ApplyCorrectedSheetCommand;
import com.buddyfantasy.rating.application.usecases.closeratingwindow.CloseRatingWindowCommand;
import com.buddyfantasy.rating.application.usecases.openratingwindow.OpenRatingWindowCommand;
import com.buddyfantasy.rating.domain.SheetPlayer;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The rating window of a match from the real events (RF-16, RF-17, RF-22, RF-23): {@code MatchClosed} opens it with
 * the league's parameters, frozen until the end; the job closes it once its time is up and publishes
 * {@code RatingWindowClosed} once; {@code MatchCorrected} replaces the players before and after the close. The votes
 * are written straight into the table, as the members' ones would be. The job is run by hand and the listeners are
 * awaited.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupLockTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class RatingFlowTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** After the kickoff, when the admin closes the match. */
    static final Duration CLOSE_AFTER_KICKOFF = Duration.ofHours(2);
    /** The default rating window. */
    static final Duration WINDOW = Duration.ofHours(24);

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    CloseDueRatingWindowsJob closeJob;

    @Autowired
    RecordedEvents events;

    UUID admin;
    UUID ana;
    UUID bea;
    TestLeague peña;
    /** Goalkeeper, defender, midfielder and forward. */
    List<UUID> squad;
    UUID match1;
    UUID match2;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        bea = fixtures.user("bea");
        peña = fixtures.league(admin);
        fixtures.join(peña, ana, bea);
        squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD);
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
    }

    /** Closes the match after kickoff with the first {@code players} of the squad, and returns when it was closed. */
    private Instant close(UUID matchId, Instant kickoffAt, int players) {
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(peña, matchId, 1, 0, lines(squad.subList(0, players)));
        return clock.now;
    }

    /** Corrects the sheet of a closed match with {@code playerIds}, an hour later. */
    private void correct(UUID matchId, List<UUID> playerIds) {
        clock.now = clock.now.plus(Duration.ofHours(1));
        fixtures.sheet(peña, matchId, 2, 0, lines(playerIds));
        Listeners.await(jdbc);
    }

    private static PerformanceLine[] lines(List<UUID> playerIds) {
        return playerIds.stream().map(playerId -> played(playerId, 0)).toArray(PerformanceLine[]::new);
    }

    private void vote(UUID matchId, UUID userId, UUID playerId, int value) {
        jdbc.update("insert into rating_vote (match_id, player_id, user_id, value, updated_at) values (?, ?, ?, ?, ?)",
                matchId, playerId, userId, value, Timestamp.from(clock.now));
    }

    private void runJobAt(Instant at) {
        clock.now = at;
        closeJob.closeDueRatingWindows();
        Listeners.await(jdbc);
    }

    private Map<String, Object> window(UUID matchId) {
        return jdbc.queryForMap("select * from rating_window where match_id = ?", matchId);
    }

    private List<Map<String, Object>> players(UUID matchId) {
        return jdbc.queryForList("select player_id, position, votes, average, rating_points from rating_player"
                + " where match_id = ? order by player_id", matchId);
    }

    private Map<UUID, Integer> votesByPlayer(UUID matchId) {
        return jdbc.query("select player_id, count(*) from rating_vote where match_id = ? group by player_id",
                        (rs, rowNum) -> entry(rs.getObject(1, UUID.class), rs.getInt(2)), matchId).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<RatingWindowClosed> closed(UUID matchId) {
        return events.stream(RatingWindowClosed.class).filter(event -> event.matchId().equals(matchId)).toList();
    }

    private List<RatingsCorrected> corrected(UUID matchId) {
        return events.stream(RatingsCorrected.class).filter(event -> event.matchId().equals(matchId)).toList();
    }

    @Test
    @DisplayName("Closing a match opens its window with the sheet and the league's parameters")
    void closingAMatchOpensItsWindowWithTheSheetAndTheLeaguesParameters() {
        Instant closedAt = close(match1, KICKOFF_1, 2);

        assertThat(window(match1)).containsAllEntriesOf(Map.of(
                "league_id", peña.id(),
                "matchday", 1,
                "kickoff_at", Timestamp.from(KICKOFF_1),
                "opened_at", Timestamp.from(closedAt),
                "closes_at", Timestamp.from(closedAt.plus(WINDOW)),
                "format", "SPADES",
                "conversion_factor", new BigDecimal("4.00"),
                "conversion_base", new BigDecimal("-2.00"),
                "min_votes", 1,
                "sheet_at", Timestamp.from(closedAt)));
        assertThat(window(match1)).containsEntry("closed_at", null).containsEntry("default_rating", null);
        assertThat(players(match1)).extracting(row -> row.get("player_id"), row -> row.get("position"),
                        row -> row.get("votes"), row -> row.get("rating_points"))
                .containsExactlyInAnyOrder(
                        tuple(squad.get(0), "GOALKEEPER", null, null),
                        tuple(squad.get(1), "DEFENDER", null, null));
        assertThat(jdbc.queryForObject("select count(*) from rating_window where match_id = ?", Integer.class,
                match2)).as("a match not closed has no window").isZero();
    }

    @Test
    @DisplayName("The window freezes the parameters it opens with, and the next match uses the edited ones")
    void theWindowFreezesTheParametersItOpensWith() {
        fixtures.settings(peña, settings -> {
            settings.ratingFormat = RatingFormat.DECIMAL;
            settings.ratingWindowHours = 48;
            settings.ratingMinVotes = 2;
            settings.defaultRating = new BigDecimal("5");
        });
        Instant closedAt = close(match1, KICKOFF_1, 2);
        fixtures.settings(peña, settings -> {
            settings.ratingFormat = RatingFormat.SPADES;
            settings.ratingWindowHours = 12;
            settings.defaultRating = null;
        });
        Instant closed2At = close(match2, KICKOFF_2, 2);

        assertThat(window(match1)).containsEntry("format", "DECIMAL")
                .containsEntry("closes_at", Timestamp.from(closedAt.plus(Duration.ofHours(48))))
                .containsEntry("conversion_factor", new BigDecimal("1.00"))
                .containsEntry("conversion_base", new BigDecimal("0.00"))
                .containsEntry("min_votes", 2)
                .containsEntry("default_rating", new BigDecimal("5.00"));
        assertThat(window(match2)).containsEntry("format", "SPADES")
                .containsEntry("closes_at", Timestamp.from(closed2At.plus(Duration.ofHours(12))))
                .containsEntry("conversion_factor", new BigDecimal("4.00"));
    }

    @Test
    @DisplayName("The job closes the window once its time is up and publishes RatingWindowClosed once")
    void theJobClosesTheWindowOnceItsTimeIsUp() {
        Instant closedAt = close(match1, KICKOFF_1, 3);
        // spades: the goalkeeper 3 and 4 (3.5 → 12), the defender 0 (−2); the midfielder unrated (0, not −2)
        vote(match1, admin, squad.get(0), 3);
        vote(match1, ana, squad.get(0), 4);
        vote(match1, bea, squad.get(1), 0);

        runJobAt(closedAt.plus(WINDOW).minusSeconds(1));
        assertThat(window(match1)).containsEntry("closed_at", null);
        assertThat(closed(match1)).isEmpty();

        Instant closesAt = closedAt.plus(WINDOW);
        runJobAt(closesAt);

        assertThat(window(match1)).containsEntry("closed_at", Timestamp.from(closesAt));
        assertThat(closed(match1)).singleElement().satisfies(event -> {
            assertThat(event.leagueId()).isEqualTo(peña.id());
            assertThat(event.occurredAt()).isEqualTo(closesAt);
            assertThat(event.players()).containsExactlyInAnyOrder(
                    new PlayerRatingData(squad.get(0), 2, new BigDecimal("3.50"), 12),
                    new PlayerRatingData(squad.get(1), 1, new BigDecimal("0.00"), -2),
                    new PlayerRatingData(squad.get(2), 0, null, 0));
        });
        assertThat(players(match1)).extracting(row -> row.get("votes"), row -> row.get("average"),
                        row -> row.get("rating_points"))
                .containsExactlyInAnyOrderElementsOf(List.of(
                        tuple(2, new BigDecimal("3.50"), 12),
                        tuple(1, new BigDecimal("0.00"), -2),
                        tuple(0, null, 0)));
        assertThat(votesByPlayer(match1)).as("the votes are kept")
                .containsOnly(entry(squad.get(0), 2), entry(squad.get(1), 1));
    }

    @Test
    @DisplayName("Running the job or the close again writes and publishes nothing more")
    void runningTheJobOrTheCloseAgainWritesAndPublishesNothingMore() {
        Instant closedAt = close(match1, KICKOFF_1, 2);
        vote(match1, ana, squad.get(0), 2);
        runJobAt(closedAt.plus(WINDOW));
        Map<String, String> before = TableDigests.of(jdbc, TableDigests.ALWAYS_CHANGING);
        List<Object> publishedBefore = events.all();

        runJobAt(closedAt.plus(WINDOW).plus(Duration.ofMinutes(1)));
        assertThat(bus.dispatch(new CloseRatingWindowCommand(match1))).isFalse();
        Listeners.await(jdbc);

        assertThat(TableDigests.of(jdbc, TableDigests.ALWAYS_CHANGING)).isEqualTo(before);
        assertThat(events.all()).isEqualTo(publishedBefore);
    }

    @Test
    @DisplayName("A window the job reaches late closes then, and each window closes at its own time")
    void aWindowTheJobReachesLateClosesThen() {
        close(match1, KICKOFF_1, 2);
        // a week later: the window of match 1 is long due, but the job has not run
        Instant closed2At = close(match2, KICKOFF_2, 2);
        Instant late = closed2At.plus(Duration.ofMinutes(1));

        runJobAt(late);

        assertThat(window(match1)).containsEntry("closed_at", Timestamp.from(late));
        assertThat(window(match2)).containsEntry("closed_at", null);
        assertThat(closed(match1)).singleElement()
                .satisfies(event -> assertThat(event.occurredAt()).isEqualTo(late));
        assertThat(closed(match2)).isEmpty();

        runJobAt(closed2At.plus(WINDOW));

        assertThat(window(match2)).containsEntry("closed_at", Timestamp.from(closed2At.plus(WINDOW)));
        assertThat(closed(match1)).hasSize(1);
        assertThat(closed(match2)).hasSize(1);
    }

    @Test
    @DisplayName("Decimal points with a minimum of 2 votes: one vote counts as unrated and gets the default rating")
    void decimalPointsWithAMinimumOfTwoVotes() {
        fixtures.settings(peña, settings -> {
            settings.ratingFormat = RatingFormat.DECIMAL;
            settings.ratingMinVotes = 2;
            settings.defaultRating = new BigDecimal("6");
        });
        Instant closedAt = close(match1, KICKOFF_1, 3);
        // 7 and 8 (7.5 → 8); a single 9, below the minimum (the default 6); none (the default 6 too)
        vote(match1, admin, squad.get(0), 7);
        vote(match1, ana, squad.get(0), 8);
        vote(match1, bea, squad.get(1), 9);

        runJobAt(closedAt.plus(WINDOW));

        assertThat(closed(match1)).singleElement().satisfies(event -> assertThat(event.players())
                .containsExactlyInAnyOrder(
                        new PlayerRatingData(squad.get(0), 2, new BigDecimal("7.50"), 8),
                        new PlayerRatingData(squad.get(1), 1, new BigDecimal("9.00"), 6),
                        new PlayerRatingData(squad.get(2), 0, null, 6)));
    }

    @Test
    @DisplayName("A correction with the window open replaces the players and drops the votes of those removed")
    void aCorrectionWithTheWindowOpenReplacesThePlayers() {
        Instant closedAt = close(match1, KICKOFF_1, 2);
        vote(match1, ana, squad.get(0), 4);
        vote(match1, ana, squad.get(1), 1);
        vote(match1, bea, squad.get(1), 2);

        // the goalkeeper did not play after all; the midfielder did
        correct(match1, List.of(squad.get(1), squad.get(2)));

        assertThat(players(match1)).extracting(row -> row.get("player_id"))
                .containsExactlyInAnyOrder(squad.get(1), squad.get(2));
        assertThat(votesByPlayer(match1)).containsOnly(entry(squad.get(1), 2));
        assertThat(window(match1)).containsEntry("sheet_at", Timestamp.from(clock.now))
                .containsEntry("closes_at", Timestamp.from(closedAt.plus(WINDOW)))
                .containsEntry("closed_at", null);
        assertThat(corrected(match1)).isEmpty();

        runJobAt(closedAt.plus(WINDOW));

        assertThat(closed(match1)).singleElement().satisfies(event -> assertThat(event.players())
                .containsExactlyInAnyOrder(
                        new PlayerRatingData(squad.get(1), 2, new BigDecimal("1.50"), 4),
                        new PlayerRatingData(squad.get(2), 0, null, 0)));
    }

    @Test
    @DisplayName("A correction after the close publishes RatingsCorrected only if a result changes")
    void aCorrectionAfterTheClosePublishesRatingsCorrectedOnlyIfAResultChanges() {
        Instant closedAt = close(match1, KICKOFF_1, 2);
        vote(match1, ana, squad.get(0), 4);
        vote(match1, bea, squad.get(1), 1);
        runJobAt(closedAt.plus(WINDOW));

        // the same players, another result: the ratings do not change
        correct(match1, List.of(squad.get(0), squad.get(1)));
        assertThat(corrected(match1)).isEmpty();

        // the defender did not play; the midfielder did, unrated: the window is not reopened
        correct(match1, List.of(squad.get(0), squad.get(2)));

        assertThat(corrected(match1)).singleElement().satisfies(event -> {
            assertThat(event.leagueId()).isEqualTo(peña.id());
            assertThat(event.occurredAt()).isEqualTo(clock.now);
            assertThat(event.players()).containsExactlyInAnyOrder(
                    new PlayerRatingData(squad.get(0), 1, new BigDecimal("4.00"), 14),
                    new PlayerRatingData(squad.get(2), 0, null, 0));
        });
        assertThat(players(match1)).extracting(row -> row.get("player_id"), row -> row.get("rating_points"))
                .containsExactlyInAnyOrder(tuple(squad.get(0), 14),
                        tuple(squad.get(2), 0));
        assertThat(votesByPlayer(match1)).containsOnly(entry(squad.get(0), 1));
        assertThat(window(match1)).containsEntry("closed_at", Timestamp.from(closedAt.plus(WINDOW)));
    }

    /** Its MatchClosed failed and is delivered after: the correction is the safety net and the close changes nothing. */
    @Test
    @DisplayName("A correction that arrives before the close opens the window, and the late close changes nothing")
    void aCorrectionThatArrivesBeforeTheCloseOpensTheWindow() {
        Instant closedAt = KICKOFF_1.plus(CLOSE_AFTER_KICKOFF);
        Instant correctedAt = closedAt.plus(Duration.ofHours(1));
        clock.now = correctedAt;

        bus.dispatch(new ApplyCorrectedSheetCommand(peña.id(), match1, 1, KICKOFF_1,
                List.of(new SheetPlayer(squad.get(1), DEFENDER)), correctedAt));
        boolean opened = bus.dispatch(new OpenRatingWindowCommand(peña.id(), match1, 1, KICKOFF_1,
                List.of(new SheetPlayer(squad.get(0), GOALKEEPER)), closedAt));

        assertThat(opened).isFalse();
        assertThat(window(match1)).containsEntry("opened_at", Timestamp.from(correctedAt))
                .containsEntry("sheet_at", Timestamp.from(correctedAt));
        assertThat(players(match1)).extracting(row -> row.get("player_id")).containsExactly(squad.get(1));
    }
}
