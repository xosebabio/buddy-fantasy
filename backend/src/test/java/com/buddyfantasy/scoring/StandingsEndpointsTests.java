package com.buddyfantasy.scoring;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.buddyfantasy.Listeners;
import com.buddyfantasy.MutableClock;
import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.league.application.usecases.removemember.RemoveMemberCommand;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static com.buddyfantasy.datadefinition.ScoringAction.GOAL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The overall standings and those of each match (RF-32) by HTTP, from matches scored by the real events: only current
 * members, from their {@code joinedAt}, with 0 for one without a lineup or over budget, tied members sharing the
 * position, and isolated by league.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as ScoringFlowTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class StandingsEndpointsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** The league locks lineups 2 h before kickoff and the job waits a minute after that. */
    static final Duration LOCK_BEFORE_KICKOFF = Duration.ofHours(2).minusMinutes(1);
    /** After the kickoff, when the admin closes the match. */
    static final Duration CLOSE_AFTER_KICKOFF = Duration.ofHours(2);

    @Autowired
    MockMvc mvc;

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

    UUID admin;
    UUID ana;
    /** A member who never saves a lineup. */
    UUID bea;
    UUID outsider;
    TestLeague peña;
    TestLeague other;
    UUID goalkeeper;
    UUID forward1;
    UUID forward2;
    /** A forward worth 5 M, in the admin's lineup only. */
    UUID cheap;
    /** Goalkeeper, 2 defenders, 2 midfielders, forward 1 and the cheap forward: 65 M. */
    List<UUID> adminLineup;
    /** Goalkeeper, 2 defenders, 2 midfielders, forward 2 and another midfielder: 70 M. */
    List<UUID> anaLineup;
    UUID match1;
    UUID match2;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        bea = fixtures.user("bea");
        outsider = fixtures.user("outsider");
        peña = fixtures.league(admin);
        fixtures.join(peña, ana, bea);
        fixtures.settings(peña, settings -> {
            settings.lineupSize = 7;
            settings.lineupFormationEnabled = false;
            settings.lineupBudgetEnabled = true;
            settings.lineupBudget = new BigDecimal("100");
        });
        fixtures.scoringRule(peña, GOAL, 10);
        // the outsider is ADMIN of another league, never a member of "Peña"
        other = fixtures.league(outsider, "Otra");

        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD, MIDFIELDER);
        goalkeeper = squad.get(0);
        forward1 = squad.get(5);
        forward2 = squad.get(6);
        cheap = fixtures.player(peña, "Cheap", null, FORWARD, new BigDecimal("5"));
        adminLineup = List.of(goalkeeper, squad.get(1), squad.get(2), squad.get(3), squad.get(4), forward1, cheap);
        anaLineup = List.of(goalkeeper, squad.get(1), squad.get(2), squad.get(3), squad.get(4), forward2,
                squad.get(7));
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
        bus.dispatch(new SaveLineupCommand(admin, peña.id(), adminLineup));
        bus.dispatch(new SaveLineupCommand(ana, peña.id(), anaLineup));
    }

    private ResultActions standings(UUID userId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/standings", peña.id()).with(as(userId)));
    }

    private ResultActions matchday(UUID userId, UUID leagueId, UUID matchId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/standings/{matchId}", leagueId, matchId).with(as(userId)));
    }

    /**
     * Locks the lineups by the job, closes the match after kickoff with {@code lines} and then its rating window,
     * without votes.
     */
    private void play(TestLeague league, UUID matchId, Instant kickoffAt, PerformanceLine... lines) {
        clock.now = kickoffAt.minus(LOCK_BEFORE_KICKOFF);
        lockJob.lockDueLineups();
        Listeners.await(jdbc);
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(league, matchId, 1, 0, lines);
        fixtures.closeRatings(clock, matchId);
    }

    /** Admin 20, ana 20 and bea, without a lineup, 0. */
    private void playMatch1() {
        play(peña, match1, KICKOFF_1, played(goalkeeper, 1), played(forward1, 1), played(forward2, 1));
    }

    /** With a 65 M budget at the lock: admin 10 and ana, over budget, 0 instead of 20. */
    private void playMatch2() {
        fixtures.settings(peña, settings -> settings.lineupBudget = new BigDecimal("65"));
        play(peña, match2, KICKOFF_2, played(goalkeeper, 0), played(forward2, 2), played(cheap, 1));
    }

    /** Two tied members in the order of the standings (by id), then {@code last}. */
    private static List<UUID> tiedThenLast(UUID tied1, UUID tied2, UUID last) {
        return Stream.concat(Stream.of(tied1, tied2).sorted(), Stream.of(last)).toList();
    }

    /** {@code first}, then two tied members in the order of the standings (by id). */
    private static List<UUID> firstThenTied(UUID first, UUID tied1, UUID tied2) {
        return Stream.concat(Stream.of(first), Stream.of(tied1, tied2).sorted()).toList();
    }

    private static String[] ids(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toArray(String[]::new);
    }

    @Test
    @DisplayName("Before any scored match every member is in the standings with 0 points")
    void beforeAnyScoredMatchEveryMemberHas0Points() throws Exception {
        standings(bea)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].userId", containsInAnyOrder(ids(List.of(admin, ana, bea)))))
                .andExpect(jsonPath("$[*].position", contains(1, 1, 1)))
                .andExpect(jsonPath("$[*].points", contains(0, 0, 0)))
                .andExpect(jsonPath("$[*].matchdays", contains(0, 0, 0)))
                .andExpect(jsonPath("$[*].lastMatchdayPoints", contains(0, 0, 0)));
    }

    @Test
    @DisplayName("The overall standings add up the scored matches and tied members share the position")
    void theOverallStandingsAddUpTheScoredMatches() throws Exception {
        playMatch1();

        standings(bea)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", contains(ids(tiedThenLast(admin, ana, bea)))))
                .andExpect(jsonPath("$[*].position", contains(1, 1, 3)))
                .andExpect(jsonPath("$[*].points", contains(20, 20, 0)))
                .andExpect(jsonPath("$[*].matchdays", contains(1, 1, 1)))
                .andExpect(jsonPath("$[*].lastMatchdayPoints", contains(20, 20, 0)));

        playMatch2();

        standings(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", contains(ids(List.of(admin, ana, bea)))))
                .andExpect(jsonPath("$[*].position", contains(1, 2, 3)))
                .andExpect(jsonPath("$[*].points", contains(30, 20, 0)))
                .andExpect(jsonPath("$[*].matchdays", contains(2, 2, 2)))
                .andExpect(jsonPath("$[*].lastMatchdayPoints", contains(10, 0, 0)));
    }

    @Test
    @DisplayName("The standings of a match show each member's lineup and every player of the sheet")
    void theStandingsOfAMatchShowLineupsAndPlayers() throws Exception {
        playMatch1();
        playMatch2();

        matchday(bea, peña.id(), match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(match1.toString()))
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.kickoffAt").value("2026-01-11T10:00:00Z"))
                // when its rating window closed, 24 h after the match was
                .andExpect(jsonPath("$.scoredAt").value("2026-01-12T12:00:00Z"))
                .andExpect(jsonPath("$.standings[*].position", contains(1, 1, 3)))
                .andExpect(jsonPath("$.standings[*].points", contains(20, 20, 0)))
                .andExpect(jsonPath("$.standings[2].userId").value(bea.toString()))
                .andExpect(jsonPath("$.standings[2].overBudget").value(false))
                .andExpect(jsonPath("$.standings[2].playerIds.length()").value(0))
                .andExpect(jsonPath("$.standings[?(@.userId == '%s')].playerIds[*]", admin)
                        .value(containsInAnyOrder(ids(adminLineup))))
                .andExpect(jsonPath("$.players[*].id",
                        containsInAnyOrder(ids(List.of(goalkeeper, forward1, forward2)))))
                .andExpect(jsonPath("$.players[*].points", contains(10, 10, 10)))
                .andExpect(jsonPath("$.players[?(@.id == '%s')].position", goalkeeper).value(contains("GOALKEEPER")))
                .andExpect(jsonPath("$.players[?(@.id == '%s')].actionPoints", goalkeeper).value(contains(10)))
                .andExpect(jsonPath("$.players[?(@.id == '%s')].ratingPoints", goalkeeper).value(contains(0)));

        // ana is over budget: 0 points, with the mark and her lineup; bea, without a lineup, ties with her
        matchday(admin, peña.id(), match2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchday").value(2))
                .andExpect(jsonPath("$.standings[0].userId").value(admin.toString()))
                .andExpect(jsonPath("$.standings[0].points").value(10))
                .andExpect(jsonPath("$.standings[0].overBudget").value(false))
                .andExpect(jsonPath("$.standings[*].position", contains(1, 2, 2)))
                .andExpect(jsonPath("$.standings[?(@.userId == '%s')].points", ana).value(contains(0)))
                .andExpect(jsonPath("$.standings[?(@.userId == '%s')].overBudget", ana).value(contains(true)))
                .andExpect(jsonPath("$.standings[?(@.userId == '%s')].playerIds[*]", ana)
                        .value(containsInAnyOrder(ids(anaLineup))))
                // sorted by points; the goalkeeper played without scoring
                .andExpect(jsonPath("$.players[*].id", contains(ids(List.of(forward2, cheap, goalkeeper)))))
                .andExpect(jsonPath("$.players[*].points", contains(20, 10, 0)));
    }

    @Test
    @DisplayName("A removed member leaves the standings and starts from zero when back")
    void aRemovedMemberLeavesTheStandingsAndStartsFromZeroWhenBack() throws Exception {
        playMatch1();

        bus.dispatch(new RemoveMemberCommand(admin, peña.id(), ana));
        standings(admin)
                .andExpect(jsonPath("$[*].userId", contains(ids(List.of(admin, bea)))))
                .andExpect(jsonPath("$[*].points", contains(20, 0)));
        matchday(admin, peña.id(), match1)
                .andExpect(jsonPath("$.standings[*].userId", contains(ids(List.of(admin, bea)))));

        fixtures.join(peña, ana);
        standings(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", contains(ids(firstThenTied(admin, ana, bea)))))
                .andExpect(jsonPath("$[*].position", contains(1, 2, 2)))
                .andExpect(jsonPath("$[*].points", contains(20, 0, 0)))
                .andExpect(jsonPath("$[?(@.userId == '%s')].matchdays", ana).value(contains(0)))
                .andExpect(jsonPath("$[?(@.userId == '%s')].matchdays", bea).value(contains(1)));
        // she joined again after the kickoff of match 1: she is not in its standings
        matchday(ana, peña.id(), match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standings[*].userId", contains(ids(List.of(admin, bea)))));
    }

    @Test
    @DisplayName("A match of the league not scored yet is 409 and one of another league is 404")
    void aMatchNotScoredIs409AndOneOfAnotherLeagueIs404() throws Exception {
        playMatch1();
        // scored in its own league, with no lineups
        UUID foreignPlayer = fixtures.player(other, FORWARD);
        UUID foreignMatch = fixtures.schedule(other, 1, KICKOFF_1);
        clock.now = KICKOFF_1.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(other, foreignMatch, 1, 0, played(foreignPlayer, 1));
        fixtures.closeRatings(clock, foreignMatch);
        matchday(outsider, other.id(), foreignMatch).andExpect(status().isOk());

        // match 2 not even locked, then locked but not closed, then closed with its rating window open
        matchday(ana, peña.id(), match2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_SCORED"));
        clock.now = KICKOFF_2.minus(LOCK_BEFORE_KICKOFF);
        lockJob.lockDueLineups();
        Listeners.await(jdbc);
        matchday(ana, peña.id(), match2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_SCORED"));
        clock.now = KICKOFF_2.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(peña, match2, 1, 0, played(forward1, 1));
        matchday(ana, peña.id(), match2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_SCORED"));
        standings(ana).andExpect(jsonPath("$[?(@.userId == '%s')].matchdays", ana).value(contains(1)));

        matchday(ana, peña.id(), foreignMatch).andExpect(status().isNotFound());
        matchday(ana, peña.id(), UUID.randomUUID()).andExpect(status().isNotFound());
        // and the other league does not see the matches of "Peña"
        matchday(outsider, other.id(), match1).andExpect(status().isNotFound());
        mvc.perform(get("/api/leagues/{id}/standings", other.id()).with(as(outsider)))
                .andExpect(jsonPath("$[*].userId", contains(outsider.toString())))
                .andExpect(jsonPath("$[*].points", contains(0)))
                .andExpect(jsonPath("$[*].matchdays", contains(1)));
    }

    @Test
    @DisplayName("A non-member sees neither standings")
    void aNonMemberSeesNeitherStandings() throws Exception {
        playMatch1();

        standings(outsider).andExpect(status().isForbidden());
        matchday(outsider, peña.id(), match1).andExpect(status().isForbidden());
    }
}
