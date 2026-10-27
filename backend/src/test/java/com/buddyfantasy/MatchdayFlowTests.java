package com.buddyfantasy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A whole matchday of phase 1 through the API only, as the frontend will drive it, with the lock job run by hand and a
 * controllable server clock: the smoke test that rating, scoring and market will extend. The users are registered by
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

    private static String[] strings(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toArray(String[]::new);
    }

    @Test
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

        // after the match the admin records the sheet and closes it
        clock.now = KICKOFF_1.plus(Duration.ofHours(2));
        UUID scorer = squad.get(6);
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

        // a correction of the closed match, audited with the rest of its history
        clock.now = KICKOFF_1.plus(Duration.ofHours(5));
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
                .andExpect(jsonPath("$[0].occurredAt").value("2026-01-11T15:00:00Z"))
                .andExpect(jsonPath("$[0].before.result.goalsFor").value(2))
                .andExpect(jsonPath("$[0].after.result.goalsFor").value(3));
        perform(get("/api/leagues/{id}/matches/{matchId}/audit", league, match1), ana)
                .andExpect(status().isForbidden());
        // the locked lineups are the history of the matchday: the correction does not touch them
        perform(get("/api/leagues/{id}/lineups/{matchId}", league, match1), ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedAt").value("2026-01-11T08:01:00Z"))
                .andExpect(jsonPath("$.lineups.length()").value(2));
    }
}
