package com.buddyfantasy.match;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
@RecordApplicationEvents
class MatchesEndpointsTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    MatchRepository matches;

    @Autowired
    ApplicationEvents events;

    UUID admin;
    UUID ana;
    UUID outsider;
    TestLeague peña;
    UUID league;
    TestLeague other;
    UUID otherLeague;

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
    }

    /**
     * Draft result and one line written directly, so these tests do not depend on the squad; the sheet use case is
     * covered by {@link MatchSheetEndpointsTests}.
     */
    private void recordDraftSheet(UUID matchId, int goalsFor, int goalsAgainst, UUID playerId, int goals) {
        jdbc.update("update match_match set goals_for = ?, goals_against = ? where id = ?",
                goalsFor, goalsAgainst, matchId);
        jdbc.update("""
                insert into match_player_performance (match_id, player_id, position, goals, yellow_card)
                values (?, ?, 'FORWARD', ?, true)""", matchId, playerId, goals);
    }

    private void close(UUID matchId) {
        jdbc.update("update match_match set status = 'CLOSED', closed_at = ? where id = ?",
                Timestamp.from(KICKOFF.plusSeconds(7200)), matchId);
    }

    private int matchesIn(UUID leagueId) {
        return jdbc.queryForObject("select count(*) from match_match where league_id = ?", Integer.class, leagueId);
    }

    private int auditEntriesIn(UUID leagueId) {
        return jdbc.queryForObject("select count(*) from match_audit_entry where league_id = ?", Integer.class,
                leagueId);
    }

    private List<Map<String, Object>> auditOf(UUID matchId) {
        return jdbc.queryForList("""
                select actor_user_id, action, before ->> 'matchday' as before_matchday,
                       before ->> 'opponent' as before_opponent, before ->> 'kickoffAt' as before_kickoff_at,
                       after ->> 'matchday' as after_matchday, after ->> 'opponent' as after_opponent,
                       after ->> 'venue' as after_venue, after ->> 'kickoffAt' as after_kickoff_at,
                       after is null as no_after
                from match_audit_entry where match_id = ? order by occurred_at""", matchId);
    }

    private static String body(Integer matchday, String opponent, String venue, String kickoffAt) {
        return """
                {"matchday": %s, "opponent": %s, "venue": %s, "kickoffAt": %s}"""
                .formatted(matchday, quoted(opponent), quoted(venue), quoted(kickoffAt));
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    @Test
    @DisplayName("The admin schedules a match and the change is audited")
    void adminSchedulesAMatchAndTheChangeIsAudited() throws Exception {
        String location = mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(5, "  Os Ninguéns  ", "AWAY", "2026-01-11T10:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.matchday").value(5))
                .andExpect(jsonPath("$.opponent").value("Os Ninguéns"))
                .andExpect(jsonPath("$.venue").value("AWAY"))
                .andExpect(jsonPath("$.kickoffAt").value("2026-01-11T10:00:00Z"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.closedAt").doesNotExist())
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.performances.length()").value(0))
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andReturn().getResponse().getHeader("Location");

        UUID matchId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        Map<String, Object> entry = jdbc.queryForMap("""
                select actor_user_id, action, before is null as no_before, after ->> 'opponent' as opponent,
                       after ->> 'kickoffAt' as kickoff_at, after ->> 'status' as status
                from match_audit_entry where league_id = ? and match_id = ?""", league, matchId);
        assertThat(entry).containsEntry("actor_user_id", admin)
                .containsEntry("action", "SCHEDULED")
                .containsEntry("no_before", true)
                .containsEntry("opponent", "Os Ninguéns")
                .containsEntry("kickoff_at", "2026-01-11T10:00:00Z")
                .containsEntry("status", "SCHEDULED");
    }

    @Test
    @DisplayName("The kickoff may be in the past")
    void kickoffMayBeInThePast() throws Exception {
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival", "HOME", "2020-09-13T09:30:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Only the league admin can schedule")
    void onlyTheLeagueAdminCanSchedule() throws Exception {
        String request = body(1, "Rival", "HOME", "2026-01-11T10:00:00Z");
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(ana)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        // admin of another league
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());

        assertThat(matchesIn(league)).isZero();
        assertThat(auditEntriesIn(league)).isZero();
    }

    @Test
    @DisplayName("Invalid data is answered with 400")
    void invalidDataIs400() throws Exception {
        for (String request : List.of(
                body(0, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(-1, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(40000, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(null, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(1, "  ", "HOME", "2026-01-11T10:00:00Z"),
                body(1, null, "HOME", "2026-01-11T10:00:00Z"),
                body(1, "x".repeat(101), "HOME", "2026-01-11T10:00:00Z"),
                body(1, "Rival", "NEUTRAL", "2026-01-11T10:00:00Z"),
                body(1, "Rival", null, "2026-01-11T10:00:00Z"),
                body(1, "Rival", "HOME", null),
                body(1, "Rival", "HOME", "next sunday"))) {
            mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        }
        assertThat(matchesIn(league)).isZero();
        assertThat(auditEntriesIn(league)).isZero();
    }

    @Test
    @DisplayName("Matchday and kickoff are unique per league")
    void matchdayAndKickoffAreUniquePerLeague() throws Exception {
        fixtures.schedule(peña, 1, KICKOFF);

        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Otro", "HOME", "2026-01-18T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Otro", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isConflict());
        // the same matchday and kickoff are free in another league
        mvc.perform(post("/api/leagues/{id}/matches", otherLeague).with(as(outsider)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Otro", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isCreated());

        assertThat(matchesIn(league)).isEqualTo(1);
        assertThat(auditEntriesIn(league)).isEqualTo(1);
    }

    @Test
    @DisplayName("Concurrent duplicates are still 409 and leave no audit entry")
    void concurrentDuplicatesAreStill409AndLeaveNoAuditEntry() throws Exception {
        fixtures.schedule(peña, 1, KICKOFF);
        // simulate a race: the pre-checks pass, the unique indexes reject the insert
        doReturn(false).when(matches).existsMatchday(any(), any());
        doReturn(false).when(matches).existsKickoff(any(), any());

        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Otro", "HOME", "2026-01-18T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Otro", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isConflict());

        assertThat(matchesIn(league)).isEqualTo(1);
        assertThat(auditEntriesIn(league)).isEqualTo(1);
    }

    @Test
    @DisplayName("Members see the calendar by matchday")
    void membersSeeTheCalendarByMatchday() throws Exception {
        // matchday 2 was postponed after matchday 3: the calendar still follows the matchday
        fixtures.schedule(peña, 2, KICKOFF.plusSeconds(14 * 86400));
        fixtures.schedule(peña, 3, KICKOFF.plusSeconds(7 * 86400));
        fixtures.schedule(peña, 1, KICKOFF);
        fixtures.schedule(other, 4, "De otra liga", KICKOFF);

        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matchday").value(contains(1, 2, 3)))
                .andExpect(jsonPath("$[0].opponent").value("Rival 1"))
                .andExpect(jsonPath("$[0].venue").value("HOME"))
                .andExpect(jsonPath("$[0].kickoffAt").value("2026-01-11T10:00:00Z"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].result").doesNotExist());
    }

    @Test
    @DisplayName("Managers see results in the calendar only once the match is closed")
    void managersSeeResultsInTheCalendarOnlyOnceTheMatchIsClosed() throws Exception {
        UUID played = fixtures.schedule(peña, 1, KICKOFF);
        recordDraftSheet(played, 2, 1, UUID.randomUUID(), 2);
        close(played);
        UUID draft = fixtures.schedule(peña, 2, KICKOFF.plusSeconds(7 * 86400));
        recordDraftSheet(draft, 3, 0, UUID.randomUUID(), 3);

        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(ana)))
                .andExpect(jsonPath("$[0].status").value("CLOSED"))
                .andExpect(jsonPath("$[0].closedAt").value("2026-01-11T12:00:00Z"))
                .andExpect(jsonPath("$[0].result.goalsFor").value(2))
                .andExpect(jsonPath("$[0].result.goalsAgainst").value(1))
                .andExpect(jsonPath("$[1].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].result").doesNotExist());
        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(admin)))
                .andExpect(jsonPath("$[0].result.goalsFor").value(2))
                .andExpect(jsonPath("$[1].result.goalsFor").value(3));
    }

    @Test
    @DisplayName("Managers see the sheet only once the match is closed and never the warnings")
    void managersSeeTheSheetOnlyOnceTheMatchIsClosedAndNeverTheWarnings() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        UUID player = UUID.randomUUID();
        // two goals for, but the only scorer has one
        recordDraftSheet(match, 2, 0, player, 1);

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opponent").value("Rival 1"))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.performances").doesNotExist())
                .andExpect(jsonPath("$.warnings").doesNotExist());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.performances.length()").value(1))
                .andExpect(jsonPath("$.performances[0].playerId").value(player.toString()))
                .andExpect(jsonPath("$.performances[0].position").value("FORWARD"))
                .andExpect(jsonPath("$.performances[0].goals").value(1))
                .andExpect(jsonPath("$.performances[0].yellowCard").value(true))
                .andExpect(jsonPath("$.performances[0].redCard").value(false))
                .andExpect(jsonPath("$.warnings").value(contains("GOALS_MISMATCH")));

        close(match);
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(ana)))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.goalsFor").value(2))
                .andExpect(jsonPath("$.performances[0].playerId").value(player.toString()))
                .andExpect(jsonPath("$.warnings").doesNotExist());
    }

    @Test
    @DisplayName("Non-members can neither list nor see matches")
    void nonMembersCannotListOrSeeMatches() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        mvc.perform(get("/api/leagues/{id}/matches", league).with(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Matches are isolated between leagues")
    void matchesAreIsolatedBetweenLeagues() throws Exception {
        UUID foreign = fixtures.schedule(other, 1, "De otra liga", KICKOFF);

        // a member of "Peña" asking for another league's match through "Peña"'s route
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, foreign).with(as(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, UUID.randomUUID()).with(as(ana)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The admin edits the opponent and venue and the change is audited")
    void adminEditsOpponentAndVenueAndTheChangeIsAudited() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "  Os Ninguéns  ", "AWAY", "2026-01-11T10:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opponent").value("Os Ninguéns"))
                .andExpect(jsonPath("$.venue").value("AWAY"))
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.kickoffAt").value("2026-01-11T10:00:00Z"));

        assertThat(auditOf(match)).last().satisfies(entry -> assertThat(entry)
                .containsEntry("actor_user_id", admin)
                .containsEntry("action", "DETAILS_UPDATED")
                .containsEntry("before_opponent", "Rival 1")
                .containsEntry("after_opponent", "Os Ninguéns")
                .containsEntry("after_venue", "AWAY"));
        assertThat(events.stream(MatchRescheduled.class)).isEmpty();
    }

    @Test
    @DisplayName("Postponing a match publishes MatchRescheduled and is audited")
    void postponingAMatchPublishesMatchRescheduledAndIsAudited() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Rival 1", "HOME", "2026-01-25T11:30:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchday").value(2))
                .andExpect(jsonPath("$.kickoffAt").value("2026-01-25T11:30:00Z"));

        assertThat(events.stream(MatchRescheduled.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.matchId()).isEqualTo(match);
            assertThat(e.matchday()).isEqualTo(2);
            assertThat(e.previousKickoffAt()).isEqualTo(KICKOFF);
            assertThat(e.kickoffAt()).isEqualTo(Instant.parse("2026-01-25T11:30:00Z"));
        });
        assertThat(auditOf(match)).last().satisfies(entry -> assertThat(entry)
                .containsEntry("action", "RESCHEDULED")
                .containsEntry("before_matchday", "1")
                .containsEntry("before_kickoff_at", "2026-01-11T10:00:00Z")
                .containsEntry("after_matchday", "2")
                .containsEntry("after_kickoff_at", "2026-01-25T11:30:00Z"));
    }

    @Test
    @DisplayName("An update without changes leaves no audit entry")
    void anUpdateWithoutChangesLeavesNoAuditEntry() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival 1 ", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isOk());

        assertThat(auditOf(match)).extracting(entry -> entry.get("action")).containsExactly("SCHEDULED");
        assertThat(events.stream(MatchRescheduled.class)).isEmpty();
    }

    @Test
    @DisplayName("An update keeps matchday and kickoff unique per league")
    void anUpdateKeepsMatchdayAndKickoffUniquePerLeague() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        fixtures.schedule(peña, 2, KICKOFF.plusSeconds(7 * 86400));

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Rival 1", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival 1", "HOME", "2026-01-18T10:00:00Z")))
                .andExpect(status().isConflict());
        // keeping its own matchday while moving the kickoff is fine
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival 1", "HOME", "2026-01-12T19:00:00Z")))
                .andExpect(status().isOk());

        assertThat(auditOf(match)).extracting(entry -> entry.get("action"))
                .containsExactly("SCHEDULED", "RESCHEDULED");
        assertThat(events.stream(MatchRescheduled.class)).hasSize(1);
    }

    @Test
    @DisplayName("Concurrent clashes on update are still 409 and leave no audit entry")
    void concurrentClashesOnUpdateAreStill409AndLeaveNoAuditEntry() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        fixtures.schedule(peña, 2, KICKOFF.plusSeconds(7 * 86400));
        // simulate a race: the pre-checks pass, the unique indexes reject the update
        doReturn(false).when(matches).existsMatchday(any(), any());
        doReturn(false).when(matches).existsKickoff(any(), any());

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Rival 1", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival 1", "HOME", "2026-01-18T10:00:00Z")))
                .andExpect(status().isConflict());

        assertThat(auditOf(match)).hasSize(1);
        assertThat(events.stream(MatchRescheduled.class)).isEmpty();
    }

    @Test
    @DisplayName("A closed match keeps its matchday and kickoff but its details can change")
    void aClosedMatchKeepsItsMatchdayAndKickoffButItsDetailsCanChange() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        recordDraftSheet(match, 1, 0, UUID.randomUUID(), 1);
        close(match);

        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Rival 1", "HOME", "2026-01-11T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Rival 1", "HOME", "2026-01-18T10:00:00Z")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(1, "Os Ninguéns", "AWAY", "2026-01-11T10:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.opponent").value("Os Ninguéns"))
                .andExpect(jsonPath("$.venue").value("AWAY"))
                .andExpect(jsonPath("$.result.goalsFor").value(1))
                .andExpect(jsonPath("$.performances.length()").value(1));

        assertThat(auditOf(match)).extracting(entry -> entry.get("action"))
                .containsExactly("SCHEDULED", "DETAILS_UPDATED");
        assertThat(events.stream(MatchRescheduled.class)).isEmpty();
    }

    @Test
    @DisplayName("Invalid updates are answered with 400")
    void invalidUpdatesAre400() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);

        for (String request : List.of(
                body(0, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(null, "Rival", "HOME", "2026-01-11T10:00:00Z"),
                body(1, "  ", "HOME", "2026-01-11T10:00:00Z"),
                body(1, "x".repeat(101), "HOME", "2026-01-11T10:00:00Z"),
                body(1, "Rival", "NEUTRAL", "2026-01-11T10:00:00Z"),
                body(1, "Rival", "HOME", null))) {
            mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        }
        assertThat(auditOf(match)).hasSize(1);
    }

    @Test
    @DisplayName("The admin deletes a scheduled match and its history is kept")
    void adminDeletesAScheduledMatchAndItsHistoryIsKept() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        recordDraftSheet(match, 1, 0, UUID.randomUUID(), 1);

        mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(matchesIn(league)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from match_player_performance where match_id = ?",
                Integer.class, match)).isZero();
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(status().isNotFound());
        assertThat(auditOf(match)).extracting(entry -> entry.get("action")).containsExactly("SCHEDULED", "DELETED");
        assertThat(auditOf(match)).last().satisfies(entry -> assertThat(entry)
                .containsEntry("actor_user_id", admin)
                .containsEntry("before_opponent", "Rival 1")
                .containsEntry("no_after", true));
        assertThat(events.stream(MatchDeleted.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.matchId()).isEqualTo(match);
            assertThat(e.matchday()).isEqualTo(1);
        });
        // the matchday and the kickoff are free again
        fixtures.schedule(peña, 1, KICKOFF);
    }

    @Test
    @DisplayName("A closed match is never deleted")
    void aClosedMatchIsNeverDeleted() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        recordDraftSheet(match, 1, 0, UUID.randomUUID(), 1);
        close(match);

        mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(matchesIn(league)).isEqualTo(1);
        assertThat(auditOf(match)).hasSize(1);
        assertThat(events.stream(MatchDeleted.class)).isEmpty();
    }

    @Test
    @DisplayName("Only the league admin can update or delete")
    void onlyTheLeagueAdminCanUpdateOrDelete() throws Exception {
        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        String request = body(2, "Otro", "AWAY", "2026-01-18T10:00:00Z");

        // a manager of the league and the admin of another league
        for (UUID user : List.of(ana, outsider)) {
            mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(user)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, match).with(as(user)).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.opponent").value("Rival 1"));
        assertThat(auditOf(match)).hasSize(1);
        assertThat(events.stream(MatchRescheduled.class)).isEmpty();
        assertThat(events.stream(MatchDeleted.class)).isEmpty();
    }

    @Test
    @DisplayName("Matches cannot be updated or deleted through another league")
    void matchesCannotBeUpdatedOrDeletedThroughAnotherLeague() throws Exception {
        UUID foreign = fixtures.schedule(other, 1, "De otra liga", KICKOFF);

        // the admin of "Peña" using their own league's route with another league's match
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, foreign).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(2, "Otro", "AWAY", "2026-01-18T10:00:00Z")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, foreign).with(as(admin)).with(csrf()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/leagues/{id}/matches/{m}", otherLeague, foreign).with(as(outsider)))
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.opponent").value("De otra liga"));
        assertThat(auditOf(foreign)).hasSize(1);
    }

    @Test
    @DisplayName("Requires a session and a CSRF token")
    void securityWithoutSessionOrCsrf() throws Exception {

        UUID match = fixtures.schedule(peña, 1, KICKOFF);
        String request = body(2, "Otro", "HOME", "2026-01-18T10:00:00Z");

        mvc.perform(get("/api/leagues/{id}/matches", league)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leagues/{id}/matches/{m}", league, match)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues/{id}/matches", league).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues/{id}/matches", league).with(as(admin))
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/matches/{m}", league, match).with(as(admin))
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, match).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/matches/{m}", league, match).with(as(admin)))
                .andExpect(status().isForbidden());
        assertThat(matchesIn(league)).isEqualTo(1);
        assertThat(auditOf(match)).hasSize(1);
    }
}
