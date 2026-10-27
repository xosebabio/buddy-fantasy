package com.buddyfantasy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.LineupsLocked;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Races against the real database: the same request sent at once (a double tap, two admins) and the two paths that
 * lock a match's lineups. The checks before writing do not see the other transaction, so the unique indexes decide:
 * each race ends in one write and a 409 or nothing, never a 500 nor a duplicate. Each case runs {@link #ROUNDS}
 * times, since a single run may not overlap.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupLockTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class ConcurrencyTests {

    static final int ROUNDS = 10;
    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant FIRST_KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

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

    @Autowired
    RecordedEvents events;

    UUID admin;
    TestLeague peña;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        peña = fixtures.league(admin);
    }

    private int status(UUID user, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(as(user)).with(csrf())).andReturn().getResponse().getStatus();
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void twoPlayersWithTheSameShirtNumberAtOnceGiveOne201AndOne409() {
        for (int shirtNumber = 1; shirtNumber <= ROUNDS; shirtNumber++) {
            String player = """
                    {"name": "Iago", "shirtNumber": %d, "position": "FORWARD", "initialValue": 10}"""
                    .formatted(shirtNumber);

            List<Integer> statuses = Concurrently.run(2, () -> status(admin,
                    post("/api/leagues/{id}/players", peña.id()).contentType(APPLICATION_JSON).content(player)));

            assertThat(statuses).as("shirt number %d", shirtNumber).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(count("select count(*) from squad_player where league_id = ?", peña.id())).isEqualTo(ROUNDS);
    }

    @Test
    void twoMatchesOnTheSameMatchdayAtOnceGiveOne201AndOne409() {
        for (int matchday = 1; matchday <= ROUNDS; matchday++) {
            Instant kickoff = FIRST_KICKOFF.plus(Duration.ofDays(7L * matchday));
            List<Callable<Integer>> schedules = List.of(
                    schedule(matchday, kickoff),
                    schedule(matchday, kickoff.plus(Duration.ofHours(1))));

            assertThat(Concurrently.run(schedules)).as("matchday %d", matchday).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(count("select count(*) from match_match where league_id = ?", peña.id())).isEqualTo(ROUNDS);
    }

    private Callable<Integer> schedule(int matchday, Instant kickoffAt) {
        String match = """
                {"matchday": %d, "opponent": "Rival", "venue": "HOME", "kickoffAt": "%s"}"""
                .formatted(matchday, kickoffAt);
        return () -> status(admin,
                post("/api/leagues/{id}/matches", peña.id()).contentType(APPLICATION_JSON).content(match));
    }

    @Test
    void joiningWithTheSameLinkSeveralTimesAtOnceMakesOneMembership() {
        for (int round = 1; round <= ROUNDS; round++) {
            UUID user = fixtures.user("user" + round);

            List<Integer> statuses = Concurrently.run(4, () -> status(user,
                    post("/api/invitations/{token}/join", peña.invitationToken())));

            assertThat(statuses).as("round %d", round).containsOnly(200);
            assertThat(count("select count(*) from league_membership where league_id = ? and user_id = ?",
                    peña.id(), user)).as("round %d", round).isOne();
        }
        assertThat(count("select count(*) from league_membership where league_id = ?", peña.id()))
                .isEqualTo(1 + ROUNDS);
    }

    @Test
    void theLockJobAndClosingTheMatchAtOnceLockItOnce() {
        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD);
        bus.dispatch(new SaveLineupCommand(admin, peña.id(), squad));

        for (int matchday = 1; matchday <= ROUNDS; matchday++) {
            Instant kickoff = FIRST_KICKOFF.plus(Duration.ofDays(7L * (matchday - 1)));
            UUID match = fixtures.schedule(peña, matchday, kickoff);
            fixtures.sheet(peña, match, 1, 0, played(squad.get(5), 1));
            // its lock time has passed, the next match's has not
            clock.now = kickoff.plus(Duration.ofHours(2));

            // the job, the close (whose MatchClosed listener locks it too) and a direct lock
            Concurrently.<Object>run(List.of(
                    () -> {
                        lockJob.lockDueLineups();
                        return null;
                    },
                    () -> bus.dispatch(new CloseMatchCommand(admin, peña.id(), match)),
                    () -> bus.dispatch(new LockMatchLineupsCommand(peña.id(), match))));
            Listeners.await(jdbc);

            assertThat(count("select count(*) from lineup_match_lock where match_id = ?", match))
                    .as("matchday %d", matchday).isOne();
            assertThat(count("select count(*) from lineup_locked_lineup where match_id = ?", match))
                    .as("matchday %d", matchday).isOne();
            assertThat(events.stream(LineupsLocked.class).filter(event -> event.matchId().equals(match)))
                    .as("matchday %d", matchday).hasSize(1);
        }
    }
}
