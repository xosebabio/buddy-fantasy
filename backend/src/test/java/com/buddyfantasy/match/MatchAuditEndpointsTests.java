package com.buddyfantasy.match;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** History of the admin's changes to a match (RNF-04). The changes themselves are tested with their use cases. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class MatchAuditEndpointsTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    UUID admin;
    UUID ana;
    UUID outsider;
    UUID league;
    UUID otherLeague;
    UUID forward;
    UUID keeper;

    @BeforeEach
    void setUp() {
        admin = register("admin");
        ana = register("ana");
        outsider = register("outsider");
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, "Peña"));
        league = created.id().value();
        bus.dispatch(new JoinLeagueCommand(ana, created.invitationToken()));
        // outsider is ADMIN of another league, never a member of "Peña"
        otherLeague = bus.dispatch(new CreateLeagueCommand(outsider, "Otra")).id().value();
        forward = player("Iago", Position.FORWARD);
        keeper = player("Xoán", Position.GOALKEEPER);
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private UUID player(String name, Position position) {
        return bus.dispatch(new RegisterPlayerCommand(admin, league, name, null, position, BigDecimal.TEN)).value();
    }

    private UUID schedule(UUID leagueId, UUID leagueAdmin) {
        return bus.dispatch(new ScheduleMatchCommand(leagueAdmin, leagueId, 1, "Rival", Venue.HOME, KICKOFF)).value();
    }

    private static PerformanceLine line(UUID playerId, int goals, int penaltiesSaved) {
        return new PerformanceLine(playerId, goals, 0, penaltiesSaved, 0, 0, 0, false, false, false);
    }

    private void recordSheet(UUID match, int goalsFor, PerformanceLine... lines) {
        bus.dispatch(new RecordMatchSheetCommand(admin, league, match, goalsFor, 0, List.of(lines)));
    }

    @Test
    void adminSeesEveryChangeNewestFirstWithItsBeforeAndAfter() throws Exception {
        UUID match = schedule(league, admin);
        bus.dispatch(new UpdateMatchCommand(admin, league, match, 1, "Os Ninguéns", Venue.AWAY, KICKOFF));
        bus.dispatch(new UpdateMatchCommand(admin, league, match, 2, "Os Ninguéns", Venue.AWAY,
                KICKOFF.plusSeconds(3600)));
        recordSheet(match, 1, line(forward, 1, 0));
        bus.dispatch(new CloseMatchCommand(admin, league, match));
        recordSheet(match, 2, line(forward, 2, 0), line(keeper, 0, 1));

        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, match).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action").value(contains("CORRECTED", "CLOSED", "SHEET_RECORDED",
                        "RESCHEDULED", "DETAILS_UPDATED", "SCHEDULED")))
                .andExpect(jsonPath("$[*].actorUserId").value(contains(admin.toString(), admin.toString(),
                        admin.toString(), admin.toString(), admin.toString(), admin.toString())))
                .andExpect(jsonPath("$[0].occurredAt").exists())
                // correction: the whole sheet before and after
                .andExpect(jsonPath("$[0].before.status").value("CLOSED"))
                .andExpect(jsonPath("$[0].before.result.goalsFor").value(1))
                .andExpect(jsonPath("$[0].before.performances.length()").value(1))
                .andExpect(jsonPath("$[0].after.result.goalsFor").value(2))
                .andExpect(jsonPath("$[0].after.performances.length()").value(2))
                .andExpect(jsonPath("$[0].after.performances[?(@.playerId == '%s')].position", keeper)
                        .value("GOALKEEPER"))
                .andExpect(jsonPath("$[0].after.performances[?(@.playerId == '%s')].penaltiesSaved", keeper)
                        .value(1))
                .andExpect(jsonPath("$[1].before.status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].after.status").value("CLOSED"))
                .andExpect(jsonPath("$[1].after.closedAt").exists())
                .andExpect(jsonPath("$[2].before.result").doesNotExist())
                .andExpect(jsonPath("$[2].after.result.goalsFor").value(1))
                .andExpect(jsonPath("$[3].before.matchday").value(1))
                .andExpect(jsonPath("$[3].before.kickoffAt").value("2026-01-11T10:00:00Z"))
                .andExpect(jsonPath("$[3].after.matchday").value(2))
                .andExpect(jsonPath("$[3].after.kickoffAt").value("2026-01-11T11:00:00Z"))
                .andExpect(jsonPath("$[4].before.opponent").value("Rival"))
                .andExpect(jsonPath("$[4].after.opponent").value("Os Ninguéns"))
                .andExpect(jsonPath("$[4].after.venue").value("AWAY"))
                .andExpect(jsonPath("$[5].before").doesNotExist())
                .andExpect(jsonPath("$[5].after.opponent").value("Rival"))
                .andExpect(jsonPath("$[5].after.performances.length()").value(0));
    }

    @Test
    void changesWithoutEffectAreNotInTheHistory() throws Exception {
        UUID match = schedule(league, admin);
        bus.dispatch(new UpdateMatchCommand(admin, league, match, 1, "Rival", Venue.HOME, KICKOFF));
        recordSheet(match, 1, line(forward, 1, 0));
        recordSheet(match, 1, line(forward, 1, 0));
        bus.dispatch(new CloseMatchCommand(admin, league, match));
        bus.dispatch(new CloseMatchCommand(admin, league, match));

        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, match).with(as(admin)))
                .andExpect(jsonPath("$[*].action").value(contains("CLOSED", "SHEET_RECORDED", "SCHEDULED")));
    }

    @Test
    void theHistoryOfADeletedMatchIsKept() throws Exception {
        UUID match = schedule(league, admin);
        recordSheet(match, 1, line(forward, 1, 0));
        bus.dispatch(new DeleteMatchCommand(admin, league, match));

        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, match).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action").value(contains("DELETED", "SHEET_RECORDED", "SCHEDULED")))
                .andExpect(jsonPath("$[0].before.result.goalsFor").value(1))
                .andExpect(jsonPath("$[0].before.performances.length()").value(1))
                .andExpect(jsonPath("$[0].after").doesNotExist());
    }

    @Test
    void onlyTheLeagueAdminSeesTheHistory() throws Exception {
        UUID match = schedule(league, admin);

        // a manager of the league and the admin of another league
        for (UUID user : List.of(ana, outsider)) {
            mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, match).with(as(user)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unknownMatchesAndMatchesOfAnotherLeagueAre404() throws Exception {
        UUID foreign = schedule(otherLeague, outsider);

        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, UUID.randomUUID()).with(as(admin)))
                .andExpect(status().isNotFound());
        // the admin of "Peña" using their own league's route with another league's match
        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, foreign).with(as(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", otherLeague, foreign).with(as(outsider)))
                .andExpect(jsonPath("$[*].action").value(contains("SCHEDULED")));
    }

    @Test
    void theHistoryRequiresASession() throws Exception {
        UUID match = schedule(league, admin);

        mvc.perform(get("/api/leagues/{id}/matches/{m}/audit", league, match)).andExpect(status().isUnauthorized());
    }
}
