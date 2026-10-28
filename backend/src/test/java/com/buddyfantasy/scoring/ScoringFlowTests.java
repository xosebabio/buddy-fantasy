package com.buddyfantasy.scoring;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.buddyfantasy.Listeners;
import com.buddyfantasy.MutableClock;
import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.RecordedEvents;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.scoring.application.usecases.applylockedlineups.ApplyLockedLineupsCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand;
import com.buddyfantasy.scoring.application.usecases.discardlockedlineups.DiscardLockedLineupsCommand.Cause;
import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
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
import static com.buddyfantasy.datadefinition.ScoringAction.ASSIST;
import static com.buddyfantasy.datadefinition.ScoringAction.GOAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Scoring a match from the real events (RF-15, RF-31): the sheet of {@code MatchClosed} and the lineups of
 * {@code LineupsLocked}, in either order, with the league's own table frozen when the match is first scored, and
 * the copy of the lineups dropped when the match is postponed or deleted. The lock job is run by hand and the
 * listeners are awaited.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupLockTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class ScoringFlowTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** The league locks lineups 2 h before kickoff and the job waits a minute after that. */
    static final Duration LOCK_BEFORE_KICKOFF = Duration.ofHours(2).minusMinutes(1);
    /** After the kickoff, when the admin closes the match. */
    static final Duration CLOSE_AFTER_KICKOFF = Duration.ofHours(2);

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    LockDueLineupsJob lockJob;

    @Autowired
    RecordedEvents events;

    UUID admin;
    UUID ana;
    TestLeague peña;
    /** Goalkeeper, 2 defenders, 2 midfielders, 3 forwards and a midfielder, in this order. */
    List<UUID> squad;
    /** The first 7 of {@link #squad}. */
    List<UUID> adminLineup;
    /** The first 5 of {@link #squad}, the third forward and the last midfielder. */
    List<UUID> anaLineup;
    UUID match1;
    UUID match2;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        peña = fixtures.league(admin);
        fixtures.join(peña, ana);
        squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD, FORWARD,
                FORWARD, MIDFIELDER);
        adminLineup = List.copyOf(squad.subList(0, 7));
        anaLineup = Stream.concat(squad.subList(0, 5).stream(), Stream.of(squad.get(7), squad.get(8))).toList();
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
    }

    private void save(UUID userId, List<UUID> playerIds) {
        bus.dispatch(new SaveLineupCommand(userId, peña.id(), playerIds));
    }

    /** Runs the lock job once the lock time of the match kicking off at {@code kickoffAt} has passed. */
    private void lockByJob(Instant kickoffAt) {
        clock.now = kickoffAt.minus(LOCK_BEFORE_KICKOFF);
        lockJob.lockDueLineups();
        Listeners.await(jdbc);
    }

    /** Closes the match after kickoff; if the job has not locked its lineups, the close does. */
    private void close(UUID matchId, Instant kickoffAt, PerformanceLine... lines) {
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(peña, matchId, 1, 0, lines);
    }

    private static PerformanceLine assisted(UUID playerId, int assists) {
        return new PerformanceLine(playerId, 0, 0, 0, 0, assists, 0, false, false, false);
    }

    private Map<UUID, Integer> playerPoints(UUID matchId) {
        return points("select player_id, points from scoring_player_score where match_id = ?", matchId);
    }

    private Map<UUID, Integer> managerPoints(UUID matchId) {
        return points("select user_id, points from scoring_lineup where match_id = ?", matchId);
    }

    private Map<UUID, Integer> points(String sql, UUID matchId) {
        return jdbc.query(sql, (rs, rowNum) -> entry(rs.getObject(1, UUID.class), rs.getInt(2)), matchId).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<MatchdayScored> scored(UUID matchId) {
        return events.stream(MatchdayScored.class).filter(event -> event.matchId().equals(matchId)).toList();
    }

    private Map<String, Object> scoringMatch(UUID matchId) {
        return jdbc.queryForMap("select * from scoring_match where match_id = ?", matchId);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    @DisplayName("The lock and then the close score players and managers once")
    void theLockAndThenTheCloseScorePlayersAndManagersOnce() {
        fixtures.scoringRule(peña, GOAL, 10);
        fixtures.scoringRule(peña, ASSIST, 2);
        save(admin, adminLineup);
        save(ana, anaLineup);

        lockByJob(KICKOFF_1);
        assertThat(scoringMatch(match1)).containsEntry("scored_at", null);
        assertThat(count("select count(*) from scoring_lineup where match_id = ? and points is null", match1))
                .isEqualTo(2);
        assertThat(scored(match1)).isEmpty();

        // a forward of both lineups, a midfielder of both and a forward only ana lined up; a defender of the
        // admin's lineup did not play
        close(match1, KICKOFF_1, played(squad.get(5), 1), assisted(squad.get(3), 1), played(squad.get(7), 2));

        assertThat(playerPoints(match1)).containsOnly(
                entry(squad.get(5), 10), entry(squad.get(3), 2), entry(squad.get(7), 20));
        assertThat(managerPoints(match1)).containsOnly(entry(admin, 12), entry(ana, 22));
        assertThat(scoringMatch(match1)).containsEntry("scored_at", Timestamp.from(clock.now));
        assertThat(scored(match1)).singleElement().satisfies(event -> {
            assertThat(event.leagueId()).isEqualTo(peña.id());
            assertThat(event.matchday()).isEqualTo(1);
            assertThat(event.kickoffAt()).isEqualTo(KICKOFF_1);
            assertThat(event.occurredAt()).isEqualTo(clock.now);
            assertThat(event.players()).containsExactlyInAnyOrder(
                    new PlayerScoreData(squad.get(5), FORWARD, 10, 0, 10),
                    new PlayerScoreData(squad.get(3), MIDFIELDER, 2, 0, 2),
                    new PlayerScoreData(squad.get(7), FORWARD, 20, 0, 20));
            assertThat(event.managers()).containsExactlyInAnyOrder(
                    new ManagerScoreData(admin, 12, false), new ManagerScoreData(ana, 22, false));
        });
    }

    @Test
    @DisplayName("The close that locks the lineups scores once and a member without a lineup is left out")
    void theCloseThatLocksTheLineupsScoresOnceAndAMemberWithoutALineupIsLeftOut() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);

        // the job did not run: closing the match locks its lineups, after the sheet
        close(match1, KICKOFF_1, played(squad.get(5), 1));

        assertThat(playerPoints(match1)).containsOnly(entry(squad.get(5), 10));
        assertThat(managerPoints(match1)).containsOnly(entry(admin, 10));
        assertThat(scored(match1)).singleElement()
                .satisfies(event -> assertThat(event.managers()).containsExactly(new ManagerScoreData(admin, 10, false)));
    }

    @Test
    @DisplayName("The default table scores penalty goals and double yellows as the requirements say")
    void theDefaultTableScoresPenaltyGoalsAndDoubleYellowsAsTheRequirementsSay() {
        save(admin, adminLineup);
        lockByJob(KICKOFF_1);

        close(match1, KICKOFF_1,
                // the goalkeeper and a defender score a penalty: +3 each, not also a goal
                new PerformanceLine(squad.get(0), 0, 1, 0, 0, 0, 0, false, false, false),
                new PerformanceLine(squad.get(1), 0, 1, 0, 0, 0, 0, false, false, false),
                // a defender sees a yellow and a red card: −1 − 5
                new PerformanceLine(squad.get(2), 0, 0, 0, 0, 0, 0, true, false, true),
                // a midfielder sees a double yellow: −3 in total
                new PerformanceLine(squad.get(3), 0, 0, 0, 0, 0, 0, false, true, false),
                // a forward scores: +3
                played(squad.get(5), 1));

        assertThat(playerPoints(match1)).containsOnly(entry(squad.get(0), 3), entry(squad.get(1), 3),
                entry(squad.get(2), -6), entry(squad.get(3), -3), entry(squad.get(5), 3));
        assertThat(managerPoints(match1)).containsOnly(entry(admin, 0));
    }

    @Test
    @DisplayName("The table is frozen when the match is scored and the next match uses the edited one")
    void theTableIsFrozenWhenTheMatchIsScoredAndTheNextMatchUsesTheEditedOne() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);
        close(match1, KICKOFF_1, played(squad.get(5), 1));

        fixtures.scoringRule(peña, GOAL, 7);
        close(match2, KICKOFF_2, played(squad.get(5), 1));

        assertThat(managerPoints(match1)).containsOnly(entry(admin, 10));
        assertThat(managerPoints(match2)).containsOnly(entry(admin, 7));
        assertThat(jdbc.queryForObject("select rules -> 'GOAL' ->> 'FORWARD' from scoring_match where match_id = ?",
                String.class, match1)).isEqualTo("10");
    }

    @Test
    @DisplayName("An over budget lineup scores zero but its players keep their points")
    void anOverBudgetLineupScoresZeroButItsPlayersKeepTheirPoints() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);
        save(ana, anaLineup);
        // 7 players of 10 M are within the budget when saved and above it when locked
        fixtures.settings(peña, settings -> settings.lineupBudget = new BigDecimal("60"));

        close(match1, KICKOFF_1, played(squad.get(5), 1));

        assertThat(playerPoints(match1)).containsOnly(entry(squad.get(5), 10));
        assertThat(managerPoints(match1)).containsOnly(entry(admin, 0), entry(ana, 0));
        assertThat(count("select count(*) from scoring_lineup where match_id = ? and over_budget", match1))
                .isEqualTo(2);
        assertThat(scored(match1)).singleElement().satisfies(event -> assertThat(event.managers())
                .containsExactlyInAnyOrder(new ManagerScoreData(admin, 0, true), new ManagerScoreData(ana, 0, true)));
    }

    @Test
    @DisplayName("A player retired before the match scores nothing and one retired after keeps his points")
    void aPlayerRetiredBeforeTheMatchScoresNothingAndOneRetiredAfterKeepsHisPoints() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);
        // a lined-up forward leaves before the match, so he is not in its sheet
        bus.dispatch(new RetirePlayerCommand(admin, peña.id(), squad.get(6)));

        close(match1, KICKOFF_1, played(squad.get(5), 1));
        bus.dispatch(new RetirePlayerCommand(admin, peña.id(), squad.get(5)));
        Listeners.await(jdbc);

        assertThat(playerPoints(match1)).containsOnly(entry(squad.get(5), 10));
        assertThat(managerPoints(match1)).containsOnly(entry(admin, 10));
    }

    @Test
    @DisplayName("Postponing a match drops the copy of its lineups and the new lock replaces it")
    void postponingAMatchDropsTheCopyOfItsLineupsAndTheNewLockReplacesIt() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);
        lockByJob(KICKOFF_1);
        assertThat(count("select count(*) from scoring_lineup where match_id = ?", match1)).isOne();

        Instant postponed = KICKOFF_1.plus(Duration.ofDays(2));
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        bus.dispatch(new UpdateMatchCommand(admin, peña.id(), match1, 1, "Rival", Venue.HOME, postponed));
        Listeners.await(jdbc);
        assertThat(count("select count(*) from scoring_lineup where match_id = ?", match1)).isZero();
        assertThat(scoringMatch(match1))
                .containsEntry("lineups_at", null)
                .containsEntry("unlocked_at", Timestamp.from(clock.now));

        // the admin swaps a forward for the third one before the new lock
        save(admin, Stream.concat(adminLineup.subList(0, 6).stream(), Stream.of(squad.get(7))).toList());
        lockByJob(postponed);
        close(match1, postponed, played(squad.get(6), 1), played(squad.get(7), 2));

        assertThat(managerPoints(match1)).containsOnly(entry(admin, 20));
        assertThat(scoringMatch(match1)).containsEntry("kickoff_at", Timestamp.from(postponed));
        assertThat(scored(match1)).singleElement()
                .satisfies(event -> assertThat(event.kickoffAt()).isEqualTo(postponed));
    }

    @Test
    @DisplayName("A lock delivered after its unlock is ignored even if scoring had not heard of the match")
    void aLockDeliveredAfterItsUnlockIsIgnoredEvenIfScoringHadNotHeardOfTheMatch() {
        fixtures.scoringRule(peña, GOAL, 10);
        save(admin, adminLineup);
        Instant lockedAt = KICKOFF_1.minus(Duration.ofHours(1));
        Instant unlockedAt = lockedAt.plus(Duration.ofMinutes(5));

        // as if the listener of LineupsLocked had failed and the unlock had arrived first
        bus.dispatch(new DiscardLockedLineupsCommand(peña.id(), match1, Cause.UNLOCKED, unlockedAt));
        bus.dispatch(new ApplyLockedLineupsCommand(peña.id(), match1, 1, KICKOFF_1,
                List.of(new ManagerLineup(admin, adminLineup, false)), lockedAt));

        assertThat(scoringMatch(match1))
                .containsEntry("matchday", 1)
                .containsEntry("kickoff_at", Timestamp.from(KICKOFF_1))
                .containsEntry("lineups_at", null)
                .containsEntry("unlocked_at", Timestamp.from(unlockedAt));
        assertThat(count("select count(*) from scoring_lineup where match_id = ?", match1)).isZero();

        // closing the match locks its lineups again, after the unlock
        close(match1, KICKOFF_1, played(squad.get(5), 1));

        assertThat(managerPoints(match1)).containsOnly(entry(admin, 10));
        assertThat(scored(match1)).hasSize(1);
    }

    @Test
    @DisplayName("Deleting a match drops scoring's copy of it")
    void deletingAMatchDropsScoringsCopyOfIt() {
        save(admin, adminLineup);
        lockByJob(KICKOFF_1);
        assertThat(count("select count(*) from scoring_match where match_id = ?", match1)).isOne();

        bus.dispatch(new DeleteMatchCommand(admin, peña.id(), match1));
        Listeners.await(jdbc);

        assertThat(count("select count(*) from scoring_match")).isZero();
        assertThat(count("select count(*) from scoring_lineup")).isZero();
        assertThat(count("select count(*) from scoring_lineup_player")).isZero();
        assertThat(events.stream(MatchdayScored.class)).isEmpty();
    }
}
