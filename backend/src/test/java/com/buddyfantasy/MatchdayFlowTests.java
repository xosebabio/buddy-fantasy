package com.buddyfantasy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.buddyfantasy.rating.adapter.in.scheduler.CloseDueRatingWindowsJob;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.buddyfantasy.TestUsers.as;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A whole matchday of phases 1 and 2 through the API only, as the frontend will drive it, with the lock and rating
 * jobs run by hand and a controllable server clock: the smoke test that market will extend. Both members rate the
 * players while the window is open, each one seeing only their own votes; the points of the matchday (default table:
 * a forward's goal is +3; default conversion: {@code 4 × average − 2} spades) are read through the standings and
 * histories once the job closes the window, and again after a correction. The users are registered by
 * {@link TestFixtures}: the Google login itself is {@code SessionTests}'.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class MatchdayFlowTests {

    static final Instant NOW = Instant.parse("2026-01-05T18:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    /** 2 h before {@link #KICKOFF_1}, with the lock hours a league is born with. */
    static final Instant LOCK_1 = Instant.parse("2026-01-11T08:00:00Z");
    /** 24 h after match 1 is closed, with the rating window a league is born with. */
    static final Instant RATINGS_CLOSE_1 = Instant.parse("2026-01-12T12:10:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    LockDueLineupsJob job;

    @Autowired
    CloseDueRatingWindowsJob ratingsJob;

    UUID league;

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID userId) throws Exception {
        return mvc.perform(request.with(as(userId)));
    }

    private ResultActions write(MockHttpServletRequestBuilder request, UUID userId, String body) throws Exception {
        return mvc.perform(request.with(as(userId)).with(csrf()).contentType(APPLICATION_JSON).content(body));
    }

    private static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    private UUID player(UUID admin, String name, int shirtNumber, String position) throws Exception {
        return UUID.fromString(read(write(post("/api/leagues/{id}/players", league), admin, """
                {"name": "%s", "shirtNumber": %d, "position": "%s", "initialValue": 10}"""
                .formatted(name, shirtNumber, position)).andExpect(status().isCreated()), "$.id"));
    }

    private UUID match(UUID admin, int matchday, Instant kickoffAt) throws Exception {
        return UUID.fromString(read(write(post("/api/leagues/{id}/matches", league), admin, """
                {"matchday": %d, "opponent": "Rival %d", "venue": "HOME", "kickoffAt": "%s"}"""
                .formatted(matchday, matchday, kickoffAt)).andExpect(status().isCreated()), "$.id"));
    }

    private static String lineup(List<UUID> playerIds) {
        return playerIds.stream().map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "{\"playerIds\":[", "]}"));
    }

    /** {@code goalsFor}–1 with all the goals scored by {@code scorer} and the others played. */
    private static String sheet(int goalsFor, UUID scorer, List<UUID> others) {
        List<String> lines = new ArrayList<>();
        lines.add("{\"playerId\": \"%s\", \"goals\": %d}".formatted(scorer, goalsFor));
        others.forEach(id -> lines.add("{\"playerId\": \"%s\"}".formatted(id)));
        return """
                {"result": {"goalsFor": %d, "goalsAgainst": 1}, "performances": [%s]}"""
                .formatted(goalsFor, String.join(",", lines));
    }

    /** The votes of a member, {@code value} for the player before it. */
    private static String ratings(Object... playersAndValues) {
        List<String> ratings = new ArrayList<>();
        for (int i = 0; i < playersAndValues.length; i += 2) {
            ratings.add("{\"playerId\": \"%s\", \"value\": %s}"
                    .formatted(playersAndValues[i], playersAndValues[i + 1]));
        }
        return "{\"ratings\": [" + String.join(",", ratings) + "]}";
    }

    private static String[] strings(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toArray(String[]::new);
    }

    @Test
    @DisplayName("A whole matchday runs end to end through the API")
    void aWholeMatchdayThroughTheApi() throws Exception {
        clock.now = NOW;
        UUID admin = fixtures.user("admin");
        UUID ana = fixtures.user("ana");

        // the admin creates the league and shares its link; ana previews it and joins
        ResultActions created = write(post("/api/leagues"), admin, "{\"name\": \"Peña do domingo\"}")
                .andExpect(status().isCreated());
        league = UUID.fromString(read(created, "$.id"));
        String token = read(created, "$.invitationToken");
        perform(get("/api/invitations/{token}", token), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leagueName").value("Peña do domingo"))
                .andExpect(jsonPath("$.alreadyMember").value(false));
        write(post("/api/invitations/{token}/join", token), ana, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leagueId").value(league.toString()));
        perform(get("/api/me/leagues"), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leagueId").value(league.toString()))
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
        perform(get("/api/leagues/{id}/members", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", contains("ADMIN", "MANAGER")));

        // squad and calendar
        List<UUID> squad = new ArrayList<>();
        String[] positions = {"GOALKEEPER", "DEFENDER", "DEFENDER", "MIDFIELDER", "MIDFIELDER", "MIDFIELDER",
                "FORWARD", "FORWARD"};
        for (int i = 0; i < positions.length; i++) {
            squad.add(player(admin, "Xogador " + (i + 1), i + 1, positions[i]));
        }
        UUID match1 = match(admin, 1, KICKOFF_1);
        match(admin, 2, KICKOFF_2);
        perform(get("/api/leagues/{id}/players", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
        perform(get("/api/leagues/{id}/matches", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchday", contains(1, 2)));

        // both save their lineup for matchday 1; nobody sees them until they are locked
        List<UUID> anaLineup = squad.subList(0, 7);
        List<UUID> adminLineup = squad.subList(1, 8);
        write(put("/api/leagues/{id}/lineup", league), ana, lineup(anaLineup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch.matchday").value(1))
                .andExpect(jsonPath("$.targetMatch.lockAt").value(LOCK_1.toString()))
                .andExpect(jsonPath("$.totalValue").value(70.0));
        write(put("/api/leagues/{id}/lineup", league), admin, lineup(adminLineup)).andExpect(status().isOk());
        perform(get("/api/leagues/{id}/lineups/{matchId}", league, match1), ana)
                .andExpect(status().isConflict());

        // the lock time comes and the job locks the matchday
        clock.now = LOCK_1.plus(Duration.ofMinutes(1));
        job.lockDueLineups();
        for (UUID member : List.of(ana, admin)) {
            perform(get("/api/leagues/{id}/lineups/{matchId}", league, match1), member)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lockAt").value(LOCK_1.toString()))
                    .andExpect(jsonPath("$.lineups[*].userId", containsInAnyOrder(ana.toString(), admin.toString())))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].players[*].id",
                            containsInAnyOrder(strings(anaLineup))))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].overBudget").value(false));
        }
        // a save now counts for matchday 2, against the lineup locked
        perform(get("/api/leagues/{id}/lineup", league), ana)
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.changesRemaining").value(2));
        // locked but not played: nothing scored yet
        perform(get("/api/leagues/{id}/standings", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].position", contains(1, 1)))
                .andExpect(jsonPath("$[*].points", contains(0, 0)))
                .andExpect(jsonPath("$[*].matchdays", contains(0, 0)));
        perform(get("/api/leagues/{id}/standings/{matchId}", league, match1), ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_SCORED"));

        // after the match the admin records the sheet and closes it: the last forward, only in the admin's lineup,
        // scores twice and the other forward, only in ana's, does not play
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        UUID scorer = squad.get(7);
        UUID absent = squad.get(6);
        write(put("/api/leagues/{id}/matches/{matchId}/sheet", league, match1), admin,
                sheet(2, scorer, squad.subList(0, 6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // a draft is the admin's only
        perform(get("/api/leagues/{id}/matches/{matchId}", league, match1), ana)
                .andExpect(jsonPath("$.result").doesNotExist());
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        write(post("/api/leagues/{id}/matches/{matchId}/close", league, match1), admin, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").value("2026-01-11T12:10:00Z"));
        Listeners.await(jdbc);
        perform(get("/api/leagues/{id}/matches/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.result.goalsAgainst").value(1))
                .andExpect(jsonPath("$.performances.length()").value(7))
                .andExpect(jsonPath("$.performances[?(@.playerId == '" + scorer + "')].goals").value(2));
        // closed and locked, but its rating window is open: the matchday is not scored yet
        perform(get("/api/leagues/{id}/standings/{matchId}", league, match1), ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_SCORED"));
        perform(get("/api/leagues/{id}/standings", league), ana)
                .andExpect(jsonPath("$[*].matchdays", contains(0, 0)));

        // the window is open for 24 h, in spades, for the 7 players of the sheet
        perform(get("/api/leagues/{id}/ratings", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchId", contains(match1.toString())))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].closesAt").value(RATINGS_CLOSE_1.toString()))
                .andExpect(jsonPath("$[0].players").value(7))
                .andExpect(jsonPath("$[0].myRatings").value(0));
        // ana rates the scorer and the keeper, and changes her mind about the keeper; the admin rates the scorer
        UUID keeper = squad.get(0);
        clock.now = clock.now.plus(Duration.ofHours(1));
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), ana, ratings(scorer, 4, keeper, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.format").value("SPADES"))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + keeper + "')].myRating").value(3));
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), ana, ratings(scorer, 4, keeper, 2))
                .andExpect(status().isOk());
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), admin, ratings(scorer, 3))
                .andExpect(status().isOk());
        // only the players of the sheet are rated, in the format of the window
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), admin, ratings(absent, 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_IN_SHEET"));
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), admin, ratings(scorer, 5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RATING"));
        // while it is open each one sees only their own votes, without any average
        perform(get("/api/leagues/{id}/ratings/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(7))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].myRating").value(4))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + keeper + "')].myRating").value(2))
                .andExpect(jsonPath("$.players[*].votes", everyItem(nullValue())))
                .andExpect(jsonPath("$.players[*].average", everyItem(nullValue())))
                .andExpect(jsonPath("$.players[*].ratingPoints", everyItem(nullValue())));
        perform(get("/api/leagues/{id}/ratings/{matchId}", league, match1), admin)
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].myRating").value(3))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + keeper + "')].myRating", contains(nullValue())))
                .andExpect(jsonPath("$.excludedUserIds").isEmpty());
        perform(get("/api/leagues/{id}/ratings", league), ana)
                .andExpect(jsonPath("$[0].myRatings").value(2));

        // the deadline is the server's: once it passes no vote is saved, even before the job closes the window
        clock.now = RATINGS_CLOSE_1;
        write(put("/api/leagues/{id}/ratings/{matchId}", league, match1), ana, ratings(scorer, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));

        // 24 h after the close the job closes the rating window and publishes the averages
        ratingsJob.closeDueRatingWindows();
        Listeners.await(jdbc);
        perform(get("/api/leagues/{id}/ratings/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].myRating").value(4))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].votes").value(2))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].average").value(3.5))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].ratingPoints").value(12))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + keeper + "')].votes").value(1))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + keeper + "')].ratingPoints").value(6))
                // without votes, 0 points: not the −2 of 0 spades
                .andExpect(jsonPath("$.players[?(@.votes == 0)].ratingPoints", contains(0, 0, 0, 0, 0)))
                .andExpect(jsonPath("$.players[?(@.votes == 0)].average", everyItem(nullValue())));

        // the matchday is scored: the scorer gets 6 for his 2 goals and 12 for his 3.5 spades, the keeper 6 for his
        // 2 spades, so 18 for the admin and 6 for ana
        perform(get("/api/leagues/{id}/standings", league), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", contains(admin.toString(), ana.toString())))
                .andExpect(jsonPath("$[*].position", contains(1, 2)))
                .andExpect(jsonPath("$[*].points", contains(18, 6)))
                .andExpect(jsonPath("$[*].matchdays", contains(1, 1)))
                .andExpect(jsonPath("$[*].lastMatchdayPoints", contains(18, 6)));
        perform(get("/api/leagues/{id}/standings/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.scoredAt").value("2026-01-12T12:10:00Z"))
                .andExpect(jsonPath("$.standings[*].userId", contains(admin.toString(), ana.toString())))
                .andExpect(jsonPath("$.standings[*].points", contains(18, 6)))
                .andExpect(jsonPath("$.standings[0].playerIds", containsInAnyOrder(strings(adminLineup))))
                .andExpect(jsonPath("$.players.length()").value(7))
                .andExpect(jsonPath("$.players[0].id").value(scorer.toString()))
                .andExpect(jsonPath("$.players[0].position").value("FORWARD"))
                .andExpect(jsonPath("$.players[0].actionPoints").value(6))
                .andExpect(jsonPath("$.players[0].ratingPoints").value(12))
                .andExpect(jsonPath("$.players[0].points").value(18));
        perform(get("/api/leagues/{id}/managers/{userId}/history", league, admin), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].points").value(18))
                .andExpect(jsonPath("$[0].inStandings").value(true))
                .andExpect(jsonPath("$[0].players[0].id").value(scorer.toString()))
                .andExpect(jsonPath("$[0].players[0].actions.GOAL").value(6))
                .andExpect(jsonPath("$[0].players[0].ratingPoints").value(12))
                .andExpect(jsonPath("$[0].players[0].points").value(18));
        perform(get("/api/leagues/{id}/managers/{userId}/history", league, ana), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points").value(6))
                .andExpect(jsonPath("$[0].players.length()").value(7))
                .andExpect(jsonPath("$[0].players[?(@.id == '" + keeper + "')].ratingPoints").value(6))
                .andExpect(jsonPath("$[0].players[?(@.id == '" + absent + "')].played").value(false));
        perform(get("/api/leagues/{id}/players/{playerId}/points", league, scorer), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchday", contains(1)))
                .andExpect(jsonPath("$[0].actions.GOAL").value(6))
                .andExpect(jsonPath("$[0].ratingPoints").value(12))
                .andExpect(jsonPath("$[0].points").value(18));

        // a correction of the closed match, audited with the rest of its history
        clock.now = RATINGS_CLOSE_1.plus(Duration.ofHours(3));
        write(put("/api/leagues/{id}/matches/{matchId}/sheet", league, match1), admin,
                sheet(3, scorer, squad.subList(0, 6)))
                .andExpect(status().isOk());
        Listeners.await(jdbc);
        perform(get("/api/leagues/{id}/matches/{matchId}", league, match1), ana)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.goalsFor").value(3));
        perform(get("/api/leagues/{id}/matches/{matchId}/audit", league, match1), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", contains("CORRECTED", "CLOSED", "SHEET_RECORDED", "SCHEDULED")))
                .andExpect(jsonPath("$[0].actorUserId").value(admin.toString()))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-01-12T15:10:00Z"))
                .andExpect(jsonPath("$[0].before.result.goalsFor").value(2))
                .andExpect(jsonPath("$[0].after.result.goalsFor").value(3));
        perform(get("/api/leagues/{id}/matches/{matchId}/audit", league, match1), ana)
                .andExpect(status().isForbidden());
        // the locked lineups are the history of the matchday: the correction does not touch them
        perform(get("/api/leagues/{id}/lineups/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedAt").value("2026-01-11T08:01:00Z"))
                .andExpect(jsonPath("$.lineups.length()").value(2));
        // the same players played, so the window is not reopened and the ratings stand
        perform(get("/api/leagues/{id}/ratings/{matchId}", league, match1), ana)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.players[?(@.playerId == '" + scorer + "')].ratingPoints").value(12));
        // but the points are recalculated: a third goal, 9 + 12 for the admin
        perform(get("/api/leagues/{id}/standings", league), ana)
                .andExpect(jsonPath("$[*].userId", contains(admin.toString(), ana.toString())))
                .andExpect(jsonPath("$[*].points", contains(21, 6)))
                .andExpect(jsonPath("$[*].matchdays", contains(1, 1)));
        perform(get("/api/leagues/{id}/standings/{matchId}", league, match1), ana)
                .andExpect(jsonPath("$.scoredAt").value("2026-01-12T15:10:00Z"))
                .andExpect(jsonPath("$.standings[*].points", contains(21, 6)));
        perform(get("/api/leagues/{id}/managers/{userId}/history", league, admin), ana)
                .andExpect(jsonPath("$[0].points").value(21))
                .andExpect(jsonPath("$[0].players[0].actions.GOAL").value(9))
                .andExpect(jsonPath("$[0].players[0].ratingPoints").value(12));
        perform(get("/api/leagues/{id}/players/{playerId}/points", league, scorer), ana)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].points").value(21));
    }
}
