package com.buddyfantasy.rating;

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
import com.buddyfantasy.rating.adapter.in.scheduler.CloseDueRatingWindowsJob;
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
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-41: the admin cancels a member's ratings of a match while its window is open, by HTTP with a controllable server
 * clock: the member's votes are deleted, they can no longer rate that match and the close does not count them. The
 * admin sees no votes, only whom they excluded. The job is run by hand and the listeners are awaited.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class RatingExclusionTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** After the kickoff, when the admin closes the match. */
    static final Duration CLOSE_AFTER_KICKOFF = Duration.ofHours(2);
    /** The default rating window. */
    static final Duration WINDOW = Duration.ofHours(24);

    @Autowired
    MockMvc mvc;

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
    UUID outsider;
    TestLeague peña;
    UUID league;
    UUID keeper;
    UUID defender;
    UUID midfielder;
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
        league = peña.id();
        fixtures.join(peña, ana, bea);
        // outsider is ADMIN of another league, never a member of "Peña"
        fixtures.league(outsider, "Otra");
        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, MIDFIELDER);
        keeper = squad.get(0);
        defender = squad.get(1);
        midfielder = squad.get(2);
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
    }

    /** Closes the match after kickoff with the whole squad, and returns when its window closes. */
    private Instant close(UUID matchId, Instant kickoffAt) {
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(peña, matchId, 1, 0, played(keeper, 0), played(defender, 0), played(midfielder, 0));
        return clock.now.plus(WINDOW);
    }

    private Instant closeMatch1() {
        return close(match1, KICKOFF_1);
    }

    private void runJobAt(Instant at) {
        clock.now = at;
        closeJob.closeDueRatingWindows();
        Listeners.await(jdbc);
    }

    private ResultActions exclude(UUID adminId, UUID matchId, UUID userId) throws Exception {
        return mvc.perform(put("/api/leagues/{id}/ratings/{matchId}/exclusions/{userId}", league, matchId, userId)
                .with(as(adminId)).with(csrf()));
    }

    private ResultActions save(UUID userId, UUID matchId, Map<UUID, Integer> ratings) throws Exception {
        String body = ratings.entrySet().stream()
                .map(rating -> "{\"playerId\":\"%s\",\"value\":%d}".formatted(rating.getKey(), rating.getValue()))
                .collect(Collectors.joining(",", "{\"ratings\":[", "]}"));
        return mvc.perform(put("/api/leagues/{id}/ratings/{matchId}", league, matchId).with(as(userId)).with(csrf())
                .contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions ratings(UUID userId, UUID matchId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/ratings/{matchId}", league, matchId).with(as(userId)));
    }

    private ResultActions windows(UUID userId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/ratings", league).with(as(userId)));
    }

    private Map<UUID, Integer> storedVotes(UUID userId, UUID matchId) {
        return jdbc.query("select player_id, value from rating_vote where user_id = ? and match_id = ?",
                        (rs, rowNum) -> entry(rs.getObject(1, UUID.class), rs.getInt(2)), userId, matchId).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private int exclusions() {
        return jdbc.queryForObject("select count(*) from rating_exclusion", Integer.class);
    }

    private Map<String, String> snapshot() {
        return TableDigests.of(jdbc, TableDigests.ALWAYS_CHANGING);
    }

    @Test
    @DisplayName("The admin excludes a manager with votes: they are deleted, he sees it and can no longer rate")
    void theAdminExcludesAManagerWithVotes() throws Exception {
        closeMatch1();
        save(ana, match1, Map.of(keeper, 4, defender, 1)).andExpect(status().isOk());
        save(bea, match1, Map.of(keeper, 2)).andExpect(status().isOk());
        Instant excludedAt = clock.now.plus(Duration.ofHours(1));
        clock.now = excludedAt;

        exclude(admin, match1, ana).andExpect(status().isNoContent());

        assertThat(storedVotes(ana, match1)).isEmpty();
        assertThat(storedVotes(bea, match1)).as("another member's votes are untouched").containsOnly(entry(keeper, 2));
        assertThat(jdbc.queryForMap("select match_id, user_id, excluded_by, excluded_at from rating_exclusion"))
                .as("the audit of who cancelled whom and when")
                .containsEntry("match_id", match1)
                .containsEntry("user_id", ana)
                .containsEntry("excluded_by", admin)
                .containsEntry("excluded_at", Timestamp.from(excludedAt));
        assertThat(events.stream(RaterExcluded.class))
                .containsExactly(new RaterExcluded(league, match1, ana, excludedAt));
        ratings(ana, match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.excluded").value(true))
                .andExpect(jsonPath("$.excludedUserIds").doesNotExist())
                .andExpect(jsonPath("$.players[0].myRating").doesNotExist())
                .andExpect(jsonPath("$.players[1].myRating").doesNotExist());
        ratings(bea, match1).andExpect(jsonPath("$.excluded").value(false));
        for (Map<UUID, Integer> votes : List.of(Map.of(keeper, 3), Map.<UUID, Integer>of())) {
            save(ana, match1, votes)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RATER_EXCLUDED"));
        }
        windows(ana)
                .andExpect(jsonPath("$[0].excluded").value(true))
                .andExpect(jsonPath("$[0].myRatings").value(0));
        windows(bea).andExpect(jsonPath("$[0].excluded").value(false));
        assertThat(storedVotes(ana, match1)).isEmpty();
    }

    @Test
    @DisplayName("The close does not count the votes of an excluded manager")
    void theCloseDoesNotCountTheVotesOfAnExcludedManager() throws Exception {
        Instant closesAt = closeMatch1();
        save(ana, match1, Map.of(keeper, 4, defender, 1)).andExpect(status().isOk());
        save(bea, match1, Map.of(keeper, 2)).andExpect(status().isOk());
        exclude(admin, match1, ana).andExpect(status().isNoContent());

        runJobAt(closesAt);

        // spades: only bea's 2 for the goalkeeper (6 points); the defender is left unrated (0 points)
        ratings(bea, match1)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.players[0].votes").value(1))
                .andExpect(jsonPath("$.players[0].average").value(2.0))
                .andExpect(jsonPath("$.players[0].ratingPoints").value(6))
                .andExpect(jsonPath("$.players[1].votes").value(0))
                .andExpect(jsonPath("$.players[1].average").doesNotExist())
                .andExpect(jsonPath("$.players[1].ratingPoints").value(0));
        assertThat(events.stream(RatingWindowClosed.class)).singleElement()
                .satisfies(closed -> assertThat(closed.players()).contains(
                        new PlayerRatingData(defender, 0, null, 0)));
        ratings(ana, match1).andExpect(jsonPath("$.excluded").value(true));
    }

    @Test
    @DisplayName("Excluding a member who has not rated yet stops them from rating the match")
    void excludingAMemberWhoHasNotRatedStopsThemFromRating() throws Exception {
        closeMatch1();

        exclude(admin, match1, bea).andExpect(status().isNoContent());

        save(bea, match1, Map.of(keeper, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATER_EXCLUDED"));
        assertThat(storedVotes(bea, match1)).isEmpty();
        assertThat(events.stream(RaterExcluded.class)).hasSize(1);
    }

    @Test
    @DisplayName("Excluding the same member twice changes nothing and publishes RaterExcluded once")
    void excludingTwiceChangesNothing() throws Exception {
        closeMatch1();
        exclude(admin, match1, ana).andExpect(status().isNoContent());
        Map<String, String> before = snapshot();
        clock.now = clock.now.plus(Duration.ofMinutes(5));

        exclude(admin, match1, ana).andExpect(status().isNoContent());

        assertThat(snapshot()).isEqualTo(before);
        assertThat(events.stream(RaterExcluded.class)).hasSize(1);
    }

    @Test
    @DisplayName("Only the admin receives whom they excluded, also when saving; the admin can exclude themself")
    void onlyTheAdminReceivesTheExcludedUserIds() throws Exception {
        closeMatch1();
        save(admin, match1, Map.of(keeper, 4)).andExpect(status().isOk())
                .andExpect(jsonPath("$.excluded").value(false))
                .andExpect(jsonPath("$.excludedUserIds.length()").value(0));
        ratings(ana, match1).andExpect(jsonPath("$.excludedUserIds").doesNotExist());

        exclude(admin, match1, bea).andExpect(status().isNoContent());
        exclude(admin, match1, ana).andExpect(status().isNoContent());

        // ordered by id, as Postgres orders uuids: byte by byte, like their text
        String[] excluded = List.of(ana, bea).stream().map(UUID::toString).sorted().toArray(String[]::new);
        save(admin, match1, Map.of(keeper, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excludedUserIds", contains(excluded)))
                // no votes of anyone else, only whom
                .andExpect(jsonPath("$.players[0].myRating").value(3))
                .andExpect(jsonPath("$.players[0].votes").doesNotExist());
        ratings(admin, match1).andExpect(jsonPath("$.excludedUserIds", contains(excluded)));
        ratings(ana, match1)
                .andExpect(jsonPath("$.excluded").value(true))
                .andExpect(jsonPath("$.excludedUserIds").doesNotExist());

        exclude(admin, match1, admin).andExpect(status().isNoContent());

        assertThat(storedVotes(admin, match1)).isEmpty();
        save(admin, match1, Map.of(keeper, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATER_EXCLUDED"));
        ratings(admin, match1)
                .andExpect(jsonPath("$.excluded").value(true))
                .andExpect(jsonPath("$.excludedUserIds.length()").value(3));
    }

    @Test
    @DisplayName("The exclusion from a match does not affect another one")
    void theExclusionFromAMatchDoesNotAffectAnother() throws Exception {
        closeMatch1();
        exclude(admin, match1, ana).andExpect(status().isNoContent());

        close(match2, KICKOFF_2);

        save(ana, match2, Map.of(keeper, 3)).andExpect(status().isOk())
                .andExpect(jsonPath("$.excluded").value(false));
        ratings(ana, match2).andExpect(jsonPath("$.excluded").value(false));
        windows(ana)
                .andExpect(jsonPath("$[0].matchId").value(match2.toString()))
                .andExpect(jsonPath("$[0].excluded").value(false))
                .andExpect(jsonPath("$[1].matchId").value(match1.toString()))
                .andExpect(jsonPath("$[1].excluded").value(true));
    }

    @Test
    @DisplayName("Only the admin can exclude: a manager or a user outside the league gets 403 and nothing is written")
    void onlyTheAdminCanExclude() throws Exception {
        closeMatch1();
        save(bea, match1, Map.of(keeper, 2)).andExpect(status().isOk());
        Map<String, String> before = snapshot();

        exclude(ana, match1, bea).andExpect(status().isForbidden());
        exclude(bea, match1, bea).andExpect(status().isForbidden());
        exclude(outsider, match1, bea).andExpect(status().isForbidden());

        assertThat(snapshot()).isEqualTo(before);
        assertThat(events.stream(RaterExcluded.class)).isEmpty();
    }

    @Test
    @DisplayName("Excluding a user who is not a member, or from a match of another league, is answered with 404")
    void excludingANonMemberOrFromAnotherLeagueIs404() throws Exception {
        closeMatch1();

        exclude(admin, match1, outsider).andExpect(status().isNotFound());
        exclude(admin, match1, UUID.randomUUID()).andExpect(status().isNotFound());
        exclude(admin, UUID.randomUUID(), ana).andExpect(status().isNotFound());

        assertThat(exclusions()).isZero();
    }

    @Test
    @DisplayName("No exclusion without an open window: 409 for a match not closed, from closesAt and after the job")
    void noExclusionWithoutAnOpenWindow() throws Exception {
        exclude(admin, match1, ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_CLOSED"));
        Instant closesAt = closeMatch1();
        save(ana, match1, Map.of(keeper, 4)).andExpect(status().isOk());

        clock.now = closesAt;
        exclude(admin, match1, ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));
        runJobAt(closesAt.plus(Duration.ofMinutes(1)));
        exclude(admin, match1, ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));

        assertThat(exclusions()).isZero();
        assertThat(storedVotes(ana, match1)).containsOnly(entry(keeper, 4));
        ratings(ana, match1).andExpect(jsonPath("$.players[0].votes").value(1));
        assertThat(events.stream(RaterExcluded.class)).isEmpty();
    }
}
