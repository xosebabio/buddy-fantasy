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
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
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
import static com.buddyfantasy.datadefinition.ScoringAction.YELLOW_CARD;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The points history of a manager (RF-33) and of a player (RF-11) by HTTP, from matches scored by the real events:
 * each lined-up player with the breakdown per action of the table the match was scored with, 0 for one who did not
 * play, the matches before a member last joined marked outside the standings, and isolated by league.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as ScoringFlowTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class HistoryEndpointsTests {

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
    /** In both lineups, never in the sheet. */
    UUID defender;
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
        fixtures.scoringRule(peña, YELLOW_CARD, -1);
        // the outsider is ADMIN of another league, never a member of "Peña"
        other = fixtures.league(outsider, "Otra");

        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD, MIDFIELDER);
        goalkeeper = squad.get(0);
        defender = squad.get(1);
        forward1 = squad.get(5);
        forward2 = squad.get(6);
        cheap = fixtures.player(peña, "Cheap", null, FORWARD, new BigDecimal("5"));
        adminLineup = List.of(goalkeeper, defender, squad.get(2), squad.get(3), squad.get(4), forward1, cheap);
        anaLineup = List.of(goalkeeper, defender, squad.get(2), squad.get(3), squad.get(4), forward2, squad.get(7));
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
        bus.dispatch(new SaveLineupCommand(admin, peña.id(), adminLineup));
        bus.dispatch(new SaveLineupCommand(ana, peña.id(), anaLineup));
    }

    private ResultActions history(UUID userId, UUID managerId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/managers/{userId}/history", peña.id(), managerId)
                .with(as(userId)));
    }

    private ResultActions points(UUID userId, UUID leagueId, UUID playerId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/players/{playerId}/points", leagueId, playerId).with(as(userId)));
    }

    /**
     * Locks the lineups by the job, closes the match after kickoff with {@code lines} and then its rating window,
     * without votes.
     */
    private void play(UUID matchId, Instant kickoffAt, PerformanceLine... lines) {
        clock.now = kickoffAt.minus(LOCK_BEFORE_KICKOFF);
        lockJob.lockDueLineups();
        Listeners.await(jdbc);
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(peña, matchId, 1, 0, lines);
        fixtures.closeRatings(clock, matchId);
    }

    /** Goalkeeper 10, forward 1 a goal and a yellow card (9), forward 2 10: admin 19, ana 20 and bea 0. */
    private void playMatch1() {
        play(match1, KICKOFF_1, played(goalkeeper, 1),
                new PerformanceLine(forward1, 1, 0, 0, 0, 0, 0, true, false, false), played(forward2, 1));
    }

    /** With a 65 M budget at the lock: admin 10 and ana, over budget, 0 instead of 20. */
    private void playMatch2() {
        fixtures.settings(peña, settings -> settings.lineupBudget = new BigDecimal("65"));
        play(match2, KICKOFF_2, played(goalkeeper, 0), played(forward2, 2), played(cheap, 1));
    }

    /** {@code first} and {@code second}, then the rest of {@code lineup} in the order of the view (by id). */
    private static String[] firstThenById(UUID first, UUID second, List<UUID> lineup) {
        return Stream.concat(Stream.of(first, second),
                        lineup.stream().filter(id -> !id.equals(first) && !id.equals(second)).sorted())
                .map(UUID::toString)
                .toArray(String[]::new);
    }

    @Test
    @DisplayName("A manager's history shows each matchday with the points and breakdown of every lined-up player")
    void aManagersHistoryShowsEachMatchdayWithItsPlayers() throws Exception {
        playMatch1();
        playMatch2();

        history(bea, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchId", contains(match1.toString(), match2.toString())))
                .andExpect(jsonPath("$[*].matchday", contains(1, 2)))
                .andExpect(jsonPath("$[0].kickoffAt").value("2026-01-11T10:00:00Z"))
                .andExpect(jsonPath("$[*].points", contains(19, 10)))
                .andExpect(jsonPath("$[*].overBudget", contains(false, false)))
                .andExpect(jsonPath("$[*].inStandings", contains(true, true)))
                // sorted by points, then by id
                .andExpect(jsonPath("$[0].players[*].id", contains(firstThenById(goalkeeper, forward1, adminLineup))))
                .andExpect(jsonPath("$[0].players[*].points", contains(10, 9, 0, 0, 0, 0, 0)))
                .andExpect(jsonPath("$[0].players[1].played").value(true))
                .andExpect(jsonPath("$[0].players[1].position").value("FORWARD"))
                .andExpect(jsonPath("$[0].players[1].actions.GOAL").value(10))
                .andExpect(jsonPath("$[0].players[1].actions.YELLOW_CARD").value(-1))
                .andExpect(jsonPath("$[0].players[1].actions.length()").value(2))
                .andExpect(jsonPath("$[0].players[1].actionPoints").value(9))
                .andExpect(jsonPath("$[0].players[1].ratingPoints").value(0))
                // a lined-up player who did not play scores 0, with no position
                .andExpect(jsonPath("$[0].players[?(@.id == '%s')].played", defender).value(contains(false)))
                .andExpect(jsonPath("$[0].players[?(@.id == '%s')].position", defender).value(contains((Object) null)))
                .andExpect(jsonPath("$[0].players[?(@.id == '%s')].actions.length()", defender).value(contains(0)))
                // the goalkeeper played match 2 without scoring
                .andExpect(jsonPath("$[1].players[?(@.id == '%s')].played", goalkeeper).value(contains(true)))
                .andExpect(jsonPath("$[1].players[?(@.id == '%s')].actions.length()", goalkeeper)
                        .value(contains(0)));

        // over budget: 0 points, with the mark, and each player with what they scored
        history(admin, ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].points", contains(20, 0)))
                .andExpect(jsonPath("$[*].overBudget", contains(false, true)))
                .andExpect(jsonPath("$[1].players[0].id").value(forward2.toString()))
                .andExpect(jsonPath("$[1].players[0].points").value(20))
                .andExpect(jsonPath("$[1].players.length()").value(7));
    }

    @Test
    @DisplayName("A member without a lineup has every matchday with 0 points and no players")
    void aMemberWithoutALineupHasEveryMatchdayWith0Points() throws Exception {
        history(bea, bea)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        playMatch1();
        playMatch2();

        history(bea, bea)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchday", contains(1, 2)))
                .andExpect(jsonPath("$[*].points", contains(0, 0)))
                .andExpect(jsonPath("$[*].overBudget", contains(false, false)))
                .andExpect(jsonPath("$[*].inStandings", contains(true, true)))
                .andExpect(jsonPath("$[*].players.length()", contains(0, 0)));
    }

    @Test
    @DisplayName("A member who comes back keeps their earlier matchdays outside the standings")
    void aMemberWhoComesBackKeepsTheirEarlierMatchdaysOutsideTheStandings() throws Exception {
        playMatch1();

        bus.dispatch(new RemoveMemberCommand(admin, peña.id(), ana));
        history(admin, ana).andExpect(status().isNotFound());

        fixtures.join(peña, ana);
        history(admin, ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchId", contains(match1.toString())))
                .andExpect(jsonPath("$[0].points").value(20))
                .andExpect(jsonPath("$[0].inStandings").value(false))
                .andExpect(jsonPath("$[0].players.length()").value(7));

        // one who joins after a match without a lineup in it does not have it in their history
        UUID carla = fixtures.user("carla");
        fixtures.join(peña, carla);
        history(carla, carla)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("The history of someone who is not a member is 404 and a non-member sees none")
    void theHistoryOfANonMemberIs404AndANonMemberSeesNone() throws Exception {
        playMatch1();

        history(admin, outsider).andExpect(status().isNotFound());
        history(admin, UUID.randomUUID()).andExpect(status().isNotFound());
        history(outsider, admin).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A player's points show each matchday in which they played with the breakdown")
    void aPlayersPointsShowEachMatchdayInWhichTheyPlayed() throws Exception {
        playMatch1();
        playMatch2();

        points(bea, peña.id(), forward2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchId", contains(match1.toString(), match2.toString())))
                .andExpect(jsonPath("$[*].matchday", contains(1, 2)))
                .andExpect(jsonPath("$[1].kickoffAt").value("2026-01-18T10:00:00Z"))
                .andExpect(jsonPath("$[*].position", everyItem(is("FORWARD"))))
                .andExpect(jsonPath("$[*].actions.GOAL", contains(10, 20)))
                .andExpect(jsonPath("$[*].actionPoints", contains(10, 20)))
                .andExpect(jsonPath("$[*].ratingPoints", contains(0, 0)))
                .andExpect(jsonPath("$[*].points", contains(10, 20)));

        // forward 1 did not play match 2
        points(ana, peña.id(), forward1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchday", contains(1)))
                .andExpect(jsonPath("$[0].actions.GOAL").value(10))
                .andExpect(jsonPath("$[0].actions.YELLOW_CARD").value(-1))
                .andExpect(jsonPath("$[0].points").value(9));
    }

    @Test
    @DisplayName("A player who has not played has no points and a retired one keeps them")
    void aPlayerWhoHasNotPlayedHasNoPointsAndARetiredOneKeepsThem() throws Exception {
        points(ana, peña.id(), forward1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        playMatch1();
        bus.dispatch(new RetirePlayerCommand(admin, peña.id(), forward1));

        points(ana, peña.id(), defender)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        points(ana, peña.id(), forward1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].points", contains(9)));
    }

    @Test
    @DisplayName("The points of a player of another league are 404 and a non-member sees none")
    void thePointsOfAPlayerOfAnotherLeagueAre404() throws Exception {
        playMatch1();
        // scored in its own league, with no lineups
        UUID foreignPlayer = fixtures.player(other, FORWARD);
        UUID foreignMatch = fixtures.schedule(other, 1, KICKOFF_1);
        fixtures.closeWithSheet(other, foreignMatch, 1, 0, played(foreignPlayer, 1));
        fixtures.closeRatings(clock, foreignMatch);
        points(outsider, other.id(), foreignPlayer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        points(ana, peña.id(), foreignPlayer).andExpect(status().isNotFound());
        points(ana, peña.id(), UUID.randomUUID()).andExpect(status().isNotFound());
        points(outsider, other.id(), forward1).andExpect(status().isNotFound());
        points(outsider, peña.id(), forward1).andExpect(status().isForbidden());
    }
}
