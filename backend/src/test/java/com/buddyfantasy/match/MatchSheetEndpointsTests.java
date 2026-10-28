package com.buddyfantasy.match;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import com.buddyfantasy.squad.application.usecases.updateplayer.UpdatePlayerCommand;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Match sheet (RF-13, RF-14) and closing the match ({@link MatchClosed}). The server clock is the real one. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
@RecordApplicationEvents
class MatchSheetEndpointsTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant FAR_FUTURE = Instant.parse("2100-01-03T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    UUID admin;
    UUID ana;
    UUID outsider;
    TestLeague peña;
    UUID league;
    TestLeague other;
    UUID otherLeague;
    UUID forward;
    UUID defender;
    UUID keeper;
    UUID foreignPlayer;

    @BeforeEach
    void setUp() {
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        outsider = fixtures.user("outsider");
        peña = fixtures.league(admin);
        league = peña.id();
        fixtures.join(peña, ana);
        // outsider is ADMIN of another league, never a member of "Peña"
        other = fixtures.league(outsider, "Otra");
        otherLeague = other.id();
        forward = fixtures.player(peña, "Iago", null, Position.FORWARD);
        defender = fixtures.player(peña, "Brais", null, Position.DEFENDER);
        keeper = fixtures.player(peña, "Xoán", null, Position.GOALKEEPER);
        foreignPlayer = fixtures.player(other, "Alleo", null, Position.FORWARD);
    }

    private static String sheet(Integer goalsFor, Integer goalsAgainst, String... lines) {
        String result = goalsFor == null && goalsAgainst == null
                ? "null"
                : """
                {"goalsFor": %s, "goalsAgainst": %s}""".formatted(goalsFor, goalsAgainst);
        return """
                {"result": %s, "performances": [%s]}""".formatted(result, String.join(", ", lines));
    }

    private static String line(UUID playerId, String stats) {
        return stats.isEmpty()
                ? """
                {"playerId": "%s"}""".formatted(playerId)
                : """
                {"playerId": "%s", %s}""".formatted(playerId, stats);
    }

    private ResultActions putSheet(UUID user, UUID match, String body) throws Exception {
        return mvc.perform(put("/api/leagues/{id}/matches/{m}/sheet", league, match).with(as(user)).with(csrf())
                .contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions close(UUID user, UUID match) throws Exception {
        return mvc.perform(post("/api/leagues/{id}/matches/{m}/close", league, match).with(as(user)).with(csrf()));
    }

    private List<String> auditActionsOf(UUID matchId) {
        return jdbc.queryForList("select action from match_audit_entry where match_id = ? order by occurred_at",
                String.class, matchId);
    }

    @Test
    @DisplayName("The admin records a draft sheet with the positions from the squad")
    void adminRecordsADraftSheetWithThePositionsFromTheSquad() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        putSheet(admin, match, sheet(2, 1,
                line(forward, "\"goals\": 1, \"penaltyGoals\": 1, \"assists\": 0, \"yellowCard\": true, \"redCard\": true"),
                line(keeper, "\"penaltiesSaved\": 1, \"doubleYellow\": true"),
                line(defender, "\"assists\": 2, \"penaltiesMissed\": 1, \"ownGoals\": 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.result.goalsAgainst").value(1))
                .andExpect(jsonPath("$.performances.length()").value(3))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].position", forward).value("FORWARD"))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].goals", forward).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].penaltyGoals", forward).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].yellowCard", forward).value(true))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].redCard", forward).value(true))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].position", keeper).value("GOALKEEPER"))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].penaltiesSaved", keeper).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].doubleYellow", keeper).value(true))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].position", defender).value("DEFENDER"))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].assists", defender).value(2))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].penaltiesMissed", defender).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].ownGoals", defender).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].goals", defender).value(0))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        Map<String, Object> entry = jdbc.queryForMap("""
                select actor_user_id, after -> 'result' ->> 'goalsFor' as goals_for,
                       jsonb_array_length(after -> 'performances') as lines
                from match_audit_entry where match_id = ? and action = 'SHEET_RECORDED'""", match);
        assertThat(entry).containsEntry("actor_user_id", admin)
                .containsEntry("goals_for", "2")
                .containsEntry("lines", 3);
        assertThat(events.stream(MatchClosed.class)).isEmpty();
        assertThat(events.stream(MatchCorrected.class)).isEmpty();
    }

    @Test
    @DisplayName("A draft may have no result or no players yet")
    void aDraftMayHaveNoResultOrNoPlayersYet() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        putSheet(admin, match, sheet(null, null, line(forward, "\"goals\": 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));
        putSheet(admin, match, sheet(1, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").value(contains("GOALS_MISMATCH")));

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.result.goalsFor").value(1))
                .andExpect(jsonPath("$.performances.length()").value(0));
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED", "SHEET_RECORDED");
    }

    @Test
    @DisplayName("Warnings do not block saving")
    void warningsDoNotBlockSaving() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        // goals plus penalty goals count against goals for
        putSheet(admin, match, sheet(3, 0, line(forward, "\"goals\": 2, \"penaltyGoals\": 1")))
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // a scorer is missing and our own goals exceed the goals against
        putSheet(admin, match, sheet(3, 0, line(forward, "\"goals\": 2"), line(defender, "\"ownGoals\": 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").value(contains("GOALS_MISMATCH", "OWN_GOALS_EXCEED_GOALS_AGAINST")));

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.performances.length()").value(2))
                .andExpect(jsonPath("$.warnings").value(contains("GOALS_MISMATCH", "OWN_GOALS_EXCEED_GOALS_AGAINST")));
    }

    @Test
    @DisplayName("The same sheet again leaves no audit entry")
    void theSameSheetAgainLeavesNoAuditEntry() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1"), line(defender, "")));

        // same lines in another order
        putSheet(admin, match, sheet(1, 0, line(defender, ""), line(forward, "\"goals\": 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));

        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED");
    }

    @Test
    @DisplayName("Invalid sheets are answered with 400")
    void invalidSheetsAre400() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        String[] tooMany = IntStream.range(0, 31).mapToObj(i -> line(UUID.randomUUID(), "")).toArray(String[]::new);

        for (String request : List.of(
                sheet(1, 0, line(forward, "\"doubleYellow\": true, \"yellowCard\": true")),
                sheet(1, 0, line(forward, "\"doubleYellow\": true, \"redCard\": true")),
                sheet(1, 0, line(forward, "\"goals\": 100")),
                sheet(1, 0, line(forward, "\"assists\": -1")),
                sheet(1, 0, line(forward, "\"goals\": 1"), line(forward, "")),
                sheet(100, 0, line(forward, "")),
                sheet(1, -1, line(forward, "")),
                sheet(1, null, line(forward, "")),
                sheet(1, 0, "{\"goals\": 1}"),
                sheet(1, 0, "null"),
                sheet(1, 0, tooMany),
                "{\"result\": null}",
                // a player of another league and one that does not exist
                sheet(1, 0, line(foreignPlayer, "")),
                sheet(1, 0, line(UUID.randomUUID(), "")))) {
            putSheet(admin, match, request).andExpect(status().isBadRequest());
        }

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.performances.length()").value(0));
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED");
    }

    @Test
    @DisplayName("A retired player cannot be added but stays if already in the sheet")
    void aRetiredPlayerCannotBeAddedButStaysIfAlreadyInTheSheet() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1"))).andExpect(status().isOk());
        bus.dispatch(new RetirePlayerCommand(admin, league, forward));
        bus.dispatch(new RetirePlayerCommand(admin, league, defender));

        putSheet(admin, match, sheet(2, 0, line(forward, "\"goals\": 2"), line(defender, "")))
                .andExpect(status().isBadRequest());
        putSheet(admin, match, sheet(2, 0, line(forward, "\"goals\": 2"), line(keeper, "")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].goals", forward).value(2))
                .andExpect(jsonPath("$.performances.length()").value(2));
    }

    @Test
    @DisplayName("The position is taken when the player is added and kept afterwards")
    void thePositionIsTakenWhenThePlayerIsAddedAndKeptAfterwards() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1"))).andExpect(status().isOk());
        bus.dispatch(new UpdatePlayerCommand(admin, league, forward, "Iago", null, Position.DEFENDER, BigDecimal.TEN));

        putSheet(admin, match, sheet(2, 0, line(forward, "\"goals\": 2"))).andExpect(status().isOk());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.performances[0].position").value("FORWARD"))
                .andExpect(jsonPath("$.performances[0].goals").value(2));

        // removed and added again, the player is new to the sheet and takes the current position
        putSheet(admin, match, sheet(2, 0)).andExpect(status().isOk());
        putSheet(admin, match, sheet(2, 0, line(forward, "\"goals\": 2"))).andExpect(status().isOk());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.performances[0].position").value("DEFENDER"));
    }

    @Test
    @DisplayName("The admin closes a match and MatchClosed carries the whole sheet")
    void adminClosesAMatchAndMatchClosedCarriesTheWholeSheet() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(2, 1, line(forward, "\"goals\": 2, \"yellowCard\": true"),
                line(keeper, "\"penaltiesSaved\": 1"))).andExpect(status().isOk());

        close(admin, match)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").exists())
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.performances.length()").value(2));

        assertThat(events.stream(MatchClosed.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.matchId()).isEqualTo(match);
            assertThat(e.matchday()).isEqualTo(1);
            assertThat(e.kickoffAt()).isEqualTo(KICKOFF);
            assertThat(e.performances()).containsExactlyInAnyOrder(
                    new PlayerPerformanceData(forward, Position.FORWARD, 2, 0, 0, 0, 0, 0, true, false, false),
                    new PlayerPerformanceData(keeper, Position.GOALKEEPER, 0, 0, 1, 0, 0, 0, false, false, false));
        });
        Map<String, Object> entry = jdbc.queryForMap("""
                select actor_user_id, before ->> 'status' as before_status, after ->> 'status' as after_status,
                       after ->> 'closedAt' as closed_at
                from match_audit_entry where match_id = ? and action = 'CLOSED'""", match);
        assertThat(entry).containsEntry("actor_user_id", admin)
                .containsEntry("before_status", "SCHEDULED")
                .containsEntry("after_status", "CLOSED");
        assertThat(entry.get("closed_at")).isNotNull();
    }

    @Test
    @DisplayName("Closing twice keeps the first close and publishes once")
    void closingTwiceKeepsTheFirstCloseAndPublishesOnce() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1")));
        String closedAt = JsonPath.read(close(admin, match).andReturn().getResponse().getContentAsString(),
                "$.closedAt");

        close(admin, match)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").value(closedAt));

        assertThat(events.stream(MatchClosed.class)).hasSize(1);
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED", "CLOSED");
    }

    @Test
    @DisplayName("A match cannot be closed before kickoff")
    void aMatchCannotBeClosedBeforeKickoff() throws Exception {
        UUID match = fixtures.schedule(peña, 1, FAR_FUTURE);
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1"))).andExpect(status().isOk());

        close(admin, match).andExpect(status().isConflict());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        assertThat(events.stream(MatchClosed.class)).isEmpty();
        assertThat(auditActionsOf(match)).doesNotContain("CLOSED");
    }

    @Test
    @DisplayName("A match cannot be closed without a result or players")
    void aMatchCannotBeClosedWithoutResultOrPlayers() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        close(admin, match).andExpect(status().isConflict());
        putSheet(admin, match, sheet(1, 0)).andExpect(status().isOk());
        close(admin, match).andExpect(status().isConflict());
        putSheet(admin, match, sheet(null, null, line(forward, "\"goals\": 1"))).andExpect(status().isOk());
        close(admin, match).andExpect(status().isConflict());

        assertThat(events.stream(MatchClosed.class)).isEmpty();
        assertThat(auditActionsOf(match)).doesNotContain("CLOSED");
    }

    @Test
    @DisplayName("Managers see the sheet only once the admin closes the match")
    void managersSeeTheSheetOnlyOnceTheAdminClosesTheMatch() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet(2, 0, line(forward, "\"goals\": 1"))).andExpect(status().isOk());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(ana)))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.performances").doesNotExist())
                .andExpect(jsonPath("$.warnings").doesNotExist());
        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(ana)))
                .andExpect(jsonPath("$[0].result").doesNotExist());

        close(admin, match).andExpect(status().isOk());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(ana)))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.performances[0].playerId").value(forward.toString()))
                .andExpect(jsonPath("$.performances[0].position").value("FORWARD"))
                .andExpect(jsonPath("$.warnings").doesNotExist());
        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(ana)))
                .andExpect(jsonPath("$[0].result.goalsFor").value(2));
    }

    private UUID closedMatch(String sheet) throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        putSheet(admin, match, sheet).andExpect(status().isOk());
        close(admin, match).andExpect(status().isOk());
        return match;
    }

    @Test
    @DisplayName("Correcting a closed match publishes MatchCorrected with the whole sheet")
    void correctingAClosedMatchPublishesMatchCorrectedWithTheWholeSheet() throws Exception {
        UUID match = closedMatch(sheet(2, 1, line(forward, "\"goals\": 2"), line(keeper, "\"penaltiesSaved\": 1")));
        String closedAt = JsonPath.read(mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match)
                .with(as(admin))).andReturn().getResponse().getContentAsString(), "$.closedAt");

        // the keeper did not play after all, the defender did and one goal was an assist
        putSheet(admin, match, sheet(2, 1, line(forward, "\"goals\": 1, \"assists\": 1"),
                line(defender, "\"goals\": 1, \"yellowCard\": true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings.length()").value(0));

        assertThat(events.stream(MatchCorrected.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.matchId()).isEqualTo(match);
            assertThat(e.matchday()).isEqualTo(1);
            assertThat(e.performances()).containsExactlyInAnyOrder(
                    new PlayerPerformanceData(forward, Position.FORWARD, 1, 0, 0, 0, 1, 0, false, false, false),
                    new PlayerPerformanceData(defender, Position.DEFENDER, 1, 0, 0, 0, 0, 0, true, false, false));
        });
        assertThat(events.stream(MatchClosed.class)).hasSize(1);
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(ana)))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").value(closedAt))
                .andExpect(jsonPath("$.performances.length()").value(2))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].assists", forward).value(1))
                .andExpect(jsonPath("$.performances[?(@.playerId == '%s')].position", defender).value("DEFENDER"));
        Map<String, Object> entry = jdbc.queryForMap("""
                select actor_user_id, jsonb_array_length(before -> 'performances') as lines_before,
                       before -> 'performances' @> ?::jsonb as keeper_before,
                       after -> 'performances' @> ?::jsonb as keeper_after
                from match_audit_entry where match_id = ? and action = 'CORRECTED'""",
                "[{\"playerId\": \"" + keeper + "\"}]", "[{\"playerId\": \"" + keeper + "\"}]", match);
        assertThat(entry).containsEntry("actor_user_id", admin)
                .containsEntry("lines_before", 2)
                .containsEntry("keeper_before", true)
                .containsEntry("keeper_after", false);
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED", "CLOSED", "CORRECTED");
    }

    @Test
    @DisplayName("The same sheet on a closed match is not a correction")
    void theSameSheetOnAClosedMatchIsNotACorrection() throws Exception {
        UUID match = closedMatch(sheet(1, 0, line(forward, "\"goals\": 1"), line(defender, "")));

        putSheet(admin, match, sheet(1, 0, line(defender, ""), line(forward, "\"goals\": 1")))
                .andExpect(status().isOk());

        assertThat(events.stream(MatchCorrected.class)).isEmpty();
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED", "CLOSED");
    }

    @Test
    @DisplayName("A correction keeps the sheet complete")
    void aCorrectionKeepsTheSheetComplete() throws Exception {
        UUID match = closedMatch(sheet(1, 0, line(forward, "\"goals\": 1")));

        putSheet(admin, match, sheet(null, null, line(forward, "\"goals\": 1"))).andExpect(status().isConflict());
        putSheet(admin, match, sheet(1, 0)).andExpect(status().isConflict());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.result.goalsFor").value(1))
                .andExpect(jsonPath("$.performances.length()").value(1));
        assertThat(events.stream(MatchCorrected.class)).isEmpty();
        assertThat(auditActionsOf(match)).doesNotContain("CORRECTED");
    }

    @Test
    @DisplayName("A correction keeps players retired after the match but cannot add them")
    void aCorrectionKeepsPlayersRetiredAfterTheMatchButCannotAddThem() throws Exception {
        UUID match = closedMatch(sheet(1, 0, line(forward, "\"goals\": 1")));
        bus.dispatch(new UpdatePlayerCommand(admin, league, forward, "Iago", null, Position.MIDFIELDER, BigDecimal.TEN));
        bus.dispatch(new RetirePlayerCommand(admin, league, forward));
        bus.dispatch(new RetirePlayerCommand(admin, league, defender));

        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1"), line(defender, "")))
                .andExpect(status().isBadRequest());
        putSheet(admin, match, sheet(1, 0, line(forward, "\"goals\": 1, \"yellowCard\": true")))
                .andExpect(status().isOk());

        assertThat(events.stream(MatchCorrected.class)).singleElement().satisfies(e -> assertThat(e.performances())
                .containsExactly(new PlayerPerformanceData(forward, Position.FORWARD, 1, 0, 0, 0, 0, 0, true, false,
                        false)));
    }

    @Test
    @DisplayName("Only the league admin corrects a closed match")
    void onlyTheLeagueAdminCorrectsAClosedMatch() throws Exception {
        UUID match = closedMatch(sheet(1, 0, line(forward, "\"goals\": 1")));

        for (UUID user : List.of(ana, outsider)) {
            putSheet(user, match, sheet(2, 0, line(forward, "\"goals\": 2"))).andExpect(status().isForbidden());
        }

        assertThat(events.stream(MatchCorrected.class)).isEmpty();
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED", "CLOSED");
    }

    @Test
    @DisplayName("Only the league admin records the sheet and closes the match")
    void onlyTheLeagueAdminRecordsTheSheetAndClosesTheMatch() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        String request = sheet(1, 0, line(forward, "\"goals\": 1"));

        // a manager of the league and the admin of another league
        for (UUID user : List.of(ana, outsider)) {
            putSheet(user, match, request).andExpect(status().isForbidden());
        }
        putSheet(admin, match, request).andExpect(status().isOk());
        for (UUID user : List.of(ana, outsider)) {
            close(user, match).andExpect(status().isForbidden());
        }

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED", "SHEET_RECORDED");
        assertThat(events.stream(MatchClosed.class)).isEmpty();
    }

    @Test
    @DisplayName("Sheets cannot be recorded or closed through another league")
    void sheetsCannotBeRecordedOrClosedThroughAnotherLeague() throws Exception {
        UUID foreign = fixtures.schedule(other, 1, KICKOFF);
        mvc.perform(put("/api/leagues/{id}/matches/{m}/sheet", otherLeague, foreign).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(sheet(1, 0, line(foreignPlayer, "\"goals\": 1"))))
                .andExpect(status().isOk());

        // the admin of "Peña" using their own league's route with another league's match
        putSheet(admin, foreign, sheet(1, 0, line(forward, "\"goals\": 1"))).andExpect(status().isNotFound());
        close(admin, foreign).andExpect(status().isNotFound());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", otherLeague, foreign).with(as(outsider)))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.performances[0].playerId").value(foreignPlayer.toString()));
        assertThat(events.stream(MatchClosed.class)).isEmpty();
    }

    @Test
    @DisplayName("Requires a session and a CSRF token")
    void securityWithoutSessionOrCsrf() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        String request = sheet(1, 0, line(forward, "\"goals\": 1"));

        mvc.perform(put("/api/leagues/{id}/matches/{m}/sheet", league, match).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/matches/{m}/sheet", league, match).with(as(admin))
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(post("/api/leagues/{id}/matches/{m}/close", league, match).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues/{id}/matches/{m}/close", league, match).with(as(admin)))
                .andExpect(status().isForbidden());
        assertThat(auditActionsOf(match)).containsExactly("SCHEDULED");
    }
}
