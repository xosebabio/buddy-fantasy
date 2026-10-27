package com.buddyfantasy.lineup;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.league.application.usecases.removemember.RemoveMemberCommand;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Locking the lineups of a match (RF-25, RF-27, RF-29) by the lock job or when the match is closed, and unlocking
 * them when it is postponed or deleted, with a controllable server clock. The scheduled job is disabled in tests and
 * run by hand; the match events are awaited.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
@RecordApplicationEvents
class LineupLockTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    /** 2 h before {@link #KICKOFF_1}: the league locks lineups 2 h before kickoff. */
    static final Instant LOCK_1 = Instant.parse("2026-01-11T08:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** The job only locks matches whose lock time is at least this old ({@code lineup.lock-job.margin}). */
    static final Duration MARGIN = Duration.ofMinutes(1);

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    LockDueLineupsJob job;

    @Autowired
    ApplicationEvents events;

    UUID admin;
    UUID ana;
    UUID league;
    /** 12 active players of 10 M: 2 goalkeepers, 4 defenders, 4 midfielders and 2 forwards, in this order. */
    List<UUID> squad;
    /** The first 7 of {@link #squad}: 70 M, within the 100 M budget. */
    List<UUID> lineupA;
    /** {@link #lineupA} with its last two players replaced: 2 changes. */
    List<UUID> lineupB;
    UUID match1;
    UUID match2;

    @BeforeEach
    void setUp() {
        awaitListeners();
        clock.now = NOW;
        jdbc.update("delete from lineup_locked_player");
        jdbc.update("delete from lineup_locked_lineup");
        jdbc.update("delete from lineup_match_lock");
        jdbc.update("delete from lineup_version");
        jdbc.update("delete from match_audit_entry");
        jdbc.update("delete from match_player_performance");
        jdbc.update("delete from match_match");
        jdbc.update("delete from squad_player");
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_invitation");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
        jdbc.update("delete from identity_user");
        admin = register("admin");
        ana = register("ana");
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, "Peña"));
        league = created.id().value();
        bus.dispatch(new JoinLeagueCommand(ana, created.invitationToken()));
        jdbc.update("""
                update league_settings
                set lineup_size = 7, lineup_lock_hours = 2, lineup_max_changes = 2, lineup_formation_enabled = false,
                    lineup_budget_enabled = true, lineup_budget = 100
                where league_id = ?""", league);

        squad = new ArrayList<>();
        Stream.of(Position.GOALKEEPER, Position.GOALKEEPER, Position.DEFENDER, Position.DEFENDER,
                        Position.DEFENDER, Position.DEFENDER, Position.MIDFIELDER, Position.MIDFIELDER,
                        Position.MIDFIELDER, Position.MIDFIELDER, Position.FORWARD, Position.FORWARD)
                .forEach(position -> squad.add(bus.dispatch(new RegisterPlayerCommand(admin, league, "Player", null,
                        position, BigDecimal.TEN)).value()));
        lineupA = List.copyOf(squad.subList(0, 7));
        lineupB = Stream.concat(squad.subList(0, 5).stream(), Stream.of(squad.get(7), squad.get(8))).toList();

        match1 = schedule(1, KICKOFF_1);
        match2 = schedule(2, KICKOFF_2);
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private UUID schedule(int matchday, Instant kickoffAt) {
        return bus.dispatch(new ScheduleMatchCommand(admin, league, matchday, "Rival", Venue.HOME, kickoffAt)).value();
    }

    private void save(UUID userId, List<UUID> playerIds) {
        bus.dispatch(new SaveLineupCommand(userId, league, playerIds));
    }

    private void close(UUID matchId) {
        bus.dispatch(new RecordMatchSheetCommand(admin, league, matchId, 1, 0, List.of(
                new PerformanceLine(squad.get(10), 1, 0, 0, 0, 0, 0, false, false, false))));
        bus.dispatch(new CloseMatchCommand(admin, league, matchId));
        awaitListeners();
    }

    private void postpone(UUID matchId, int matchday, Instant kickoffAt) {
        bus.dispatch(new UpdateMatchCommand(admin, league, matchId, matchday, "Rival", Venue.HOME, kickoffAt));
        awaitListeners();
    }

    /**
     * The match events reach lineup asynchronously after commit. Waits until every listener has completed, so a
     * listener that throws fails the test instead of being missed.
     */
    private void awaitListeners() {
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject(
                "select count(*) from event_publication where completion_date is null", Integer.class) == 0);
    }

    private List<UUID> lockedMatches() {
        return jdbc.queryForList("select match_id from lineup_match_lock", UUID.class);
    }

    private List<UUID> lockedUsers(UUID matchId) {
        return jdbc.queryForList("select user_id from lineup_locked_lineup where match_id = ?", UUID.class, matchId);
    }

    private List<UUID> lockedPlayers(UUID matchId, UUID userId) {
        return jdbc.queryForList("select player_id from lineup_locked_player where match_id = ? and user_id = ?",
                UUID.class, matchId, userId);
    }

    private List<LineupsLocked> lineupsLocked() {
        return events.stream(LineupsLocked.class).toList();
    }

    private static UUID[] ids(List<UUID> playerIds) {
        return playerIds.toArray(UUID[]::new);
    }

    @Test
    void theJobLocksOnlyMatchesWhoseLockIsAMinuteOld() {
        save(ana, lineupA);

        clock.now = LOCK_1.plus(MARGIN).minusSeconds(1);
        job.lockDueLineups();
        assertThat(lockedMatches()).isEmpty();
        assertThat(lineupsLocked()).isEmpty();

        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(jdbc.queryForMap("select lock_at, locked_at from lineup_match_lock where match_id = ?", match1))
                .containsEntry("lock_at", Timestamp.from(LOCK_1))
                .containsEntry("locked_at", Timestamp.from(LOCK_1.plus(MARGIN)));
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
        assertThat(lineupsLocked()).singleElement().satisfies(event -> {
            assertThat(event.leagueId()).isEqualTo(league);
            assertThat(event.matchId()).isEqualTo(match1);
            assertThat(event.matchday()).isEqualTo(1);
            assertThat(event.kickoffAt()).isEqualTo(KICKOFF_1);
            assertThat(event.occurredAt()).isEqualTo(LOCK_1.plus(MARGIN));
            // the admin saved nothing, so has no lineup in this match
            assertThat(event.lineups()).singleElement().satisfies(lineup -> {
                assertThat(lineup.userId()).isEqualTo(ana);
                assertThat(lineup.playerIds()).containsExactlyInAnyOrder(ids(lineupA));
                assertThat(lineup.totalValue()).isEqualByComparingTo("70");
                assertThat(lineup.overBudget()).isFalse();
            });
        });
    }

    @Test
    void runningTheJobTwiceLocksAndPublishesOnce() {
        save(ana, lineupA);
        clock.now = LOCK_1.plus(MARGIN);

        job.lockDueLineups();
        job.lockDueLineups();
        boolean lockedAgain = bus.dispatch(new LockMatchLineupsCommand(league, match1));

        assertThat(lockedAgain).isFalse();
        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockedUsers(match1)).containsExactly(ana);
        assertThat(lineupsLocked()).hasSize(1);
    }

    @Test
    void aSaveAfterTheLockTimeIsNotLockedEvenIfTheJobHasNotRunYet() {
        save(ana, lineupA);
        clock.now = LOCK_1;
        save(ana, lineupB);

        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    void closingTheMatchLocksItIfTheJobDidNot() {
        save(ana, lineupA);
        save(admin, lineupB);
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));

        close(match1);

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockedUsers(match1)).containsExactlyInAnyOrder(ana, admin);
        assertThat(lineupsLocked()).singleElement().satisfies(event -> {
            assertThat(event.matchId()).isEqualTo(match1);
            assertThat(event.occurredAt()).isEqualTo(KICKOFF_1.plus(Duration.ofHours(2)));
        });
        // a closed match is no longer the job's business
        job.lockDueLineups();
        assertThat(lineupsLocked()).hasSize(1);
    }

    @Test
    void closingAMatchAlreadyLockedPublishesNothingMore() {
        save(ana, lineupA);
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        close(match1);

        assertThat(lineupsLocked()).hasSize(1);
        assertThat(jdbc.queryForObject("select locked_at from lineup_match_lock where match_id = ?",
                Timestamp.class, match1)).isEqualTo(Timestamp.from(LOCK_1.plus(MARGIN)));
    }

    @Test
    void aMatchLoadedAlreadyPlayedIsLockedWithTheVersionsSavedBeforeItsLock() {
        save(ana, lineupA);
        clock.now = Instant.parse("2026-01-20T10:00:00Z");
        save(ana, lineupB);

        // played on 15 January, between matches 1 and 2, and loaded afterwards
        UUID played = schedule(3, Instant.parse("2026-01-15T10:00:00Z"));
        close(played);

        assertThat(lockedMatches()).containsExactly(played);
        assertThat(lockedPlayers(played, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    void exMembersAreLeftOut() {
        save(ana, lineupA);
        save(admin, lineupB);
        bus.dispatch(new RemoveMemberCommand(admin, league, ana));
        clock.now = LOCK_1.plus(MARGIN);

        job.lockDueLineups();

        assertThat(lockedUsers(match1)).containsExactly(admin);
        assertThat(lineupsLocked()).singleElement()
                .satisfies(event -> assertThat(event.lineups()).extracting(LockedLineupData::userId)
                        .containsExactly(admin));
    }

    @Test
    void aLineupOverBudgetAtTheLockIsLockedButFlagged() {
        save(ana, lineupA);
        save(admin, lineupB);
        // only in ana's lineup: 60 M + 41 M
        jdbc.update("update squad_player set initial_value = 41 where id = ?", squad.get(5));
        clock.now = LOCK_1.plus(MARGIN);

        job.lockDueLineups();

        assertThat(jdbc.queryForMap(
                "select total_value, budget, over_budget from lineup_locked_lineup where match_id = ? and user_id = ?",
                match1, ana))
                .containsEntry("total_value", new BigDecimal("101.00"))
                .containsEntry("budget", new BigDecimal("100.00"))
                .containsEntry("over_budget", true);
        assertThat(jdbc.queryForObject(
                "select value from lineup_locked_player where match_id = ? and user_id = ? and player_id = ?",
                BigDecimal.class, match1, ana, squad.get(5))).isEqualByComparingTo("41");
        assertThat(lineupsLocked().getFirst().lineups())
                .filteredOn(lineup -> lineup.userId().equals(ana)).singleElement()
                .satisfies(lineup -> assertThat(lineup.overBudget()).isTrue());
        assertThat(lineupsLocked().getFirst().lineups())
                .filteredOn(lineup -> lineup.userId().equals(admin)).singleElement()
                .satisfies(lineup -> assertThat(lineup.overBudget()).isFalse());
    }

    @Test
    void aRetiredPlayerIsLockedWithTheLineupThatCarriesHim() {
        save(ana, lineupA);
        bus.dispatch(new RetirePlayerCommand(admin, league, lineupA.getLast()));
        clock.now = LOCK_1.plus(MARGIN);

        job.lockDueLineups();

        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    void postponingALockedMatchWithItsLockInTheFutureUnlocksIt() {
        save(ana, lineupA);
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();
        // counts for match 2 for now
        save(ana, lineupB);

        Instant newKickoff = Instant.parse("2026-01-13T10:00:00Z");
        postpone(match1, 1, newKickoff);

        assertThat(lockedMatches()).isEmpty();
        assertThat(events.stream(LineupsUnlocked.class)).singleElement().satisfies(event -> {
            assertThat(event.leagueId()).isEqualTo(league);
            assertThat(event.matchId()).isEqualTo(match1);
            assertThat(event.occurredAt()).isEqualTo(LOCK_1.plus(MARGIN));
        });

        // locked again at the new lock time, with what was saved before it
        clock.now = newKickoff.minus(Duration.ofHours(2)).plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupB));
        assertThat(lineupsLocked()).hasSize(2);
        assertThat(lineupsLocked().getLast().kickoffAt()).isEqualTo(newKickoff);
    }

    @Test
    void postponingALockedMatchWithItsLockStillPastChangesNothing() {
        save(ana, lineupA);
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        // kicks off an hour later, so it locks at 09:00 instead of 08:00: still past at 10:00
        clock.now = KICKOFF_1;
        postpone(match1, 1, KICKOFF_1.plus(Duration.ofHours(1)));

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
        assertThat(events.stream(LineupsUnlocked.class)).isEmpty();
    }

    @Test
    void postponingAMatchNotLockedYetChangesNothing() {
        save(ana, lineupA);

        postpone(match1, 1, Instant.parse("2026-01-13T10:00:00Z"));

        assertThat(lockedMatches()).isEmpty();
        assertThat(events.stream(LineupsUnlocked.class)).isEmpty();
    }

    @Test
    void deletingAMatchDeletesItsLockWithoutAnEvent() {
        save(ana, lineupA);
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        bus.dispatch(new DeleteMatchCommand(admin, league, match1));
        awaitListeners();

        assertThat(lockedMatches()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from lineup_locked_lineup", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from lineup_locked_player", Integer.class)).isZero();
        assertThat(events.stream(LineupsUnlocked.class)).isEmpty();
        // the versions stay: they still count for the next matches
        assertThat(jdbc.queryForObject("select count(*) from lineup_version", Integer.class)).isEqualTo(1);
    }

    @Test
    void theJobLocksEveryDueMatchAndSkipsTheOnesAhead() {
        save(ana, lineupA);
        clock.now = KICKOFF_2.minus(Duration.ofHours(2)).plus(MARGIN);
        UUID match3 = schedule(3, KICKOFF_2.plus(Duration.ofDays(7)));

        job.lockDueLineups();

        assertThat(lockedMatches()).containsExactlyInAnyOrder(match1, match2).doesNotContain(match3);
        assertThat(lineupsLocked()).extracting(LineupsLocked::matchday).containsExactly(1, 2);
    }
}
