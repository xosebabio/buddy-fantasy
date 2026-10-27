package com.buddyfantasy.squad;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import org.junit.jupiter.api.BeforeEach;
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
@RecordApplicationEvents
@Import(PostgresTestConfig.class)
class PlayersEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    @MockitoSpyBean
    PlayerRepository players;

    UUID admin;
    UUID ana;
    UUID outsider;
    UUID league;
    UUID otherLeague;

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
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private UUID addPlayer(UUID leagueId, UUID leagueAdmin, String name, Integer shirtNumber, Position position) {
        return bus.dispatch(new RegisterPlayerCommand(leagueAdmin, leagueId, name, shirtNumber, position,
                new BigDecimal("10"))).value();
    }

    private void retire(UUID playerId) {
        bus.dispatch(new RetirePlayerCommand(admin, league, playerId));
    }

    private Timestamp retiredAt(UUID playerId) {
        return jdbc.queryForObject("select retired_at from squad_player where id = ?", Timestamp.class, playerId);
    }

    private String nameOf(UUID playerId) {
        return jdbc.queryForObject("select name from squad_player where id = ?", String.class, playerId);
    }

    private int playersIn(UUID leagueId) {
        return jdbc.queryForObject("select count(*) from squad_player where league_id = ?", Integer.class, leagueId);
    }

    private static String body(String name, Integer shirtNumber, String position, String initialValue) {
        return """
                {"name": %s, "shirtNumber": %s, "position": %s, "initialValue": %s}"""
                .formatted(name == null ? "null" : "\"" + name + "\"", shirtNumber,
                        position == null ? "null" : "\"" + position + "\"", initialValue);
    }

    @Test
    void adminRegistersAPlayerAndPlayerRegisteredIsPublishedOnce() throws Exception {
        String location = mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("  Iago  ", 10, "FORWARD", "12.5")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Iago"))
                .andExpect(jsonPath("$.shirtNumber").value(10))
                .andExpect(jsonPath("$.position").value("FORWARD"))
                .andExpect(jsonPath("$.initialValue").value(12.5))
                .andExpect(jsonPath("$.currentValue").value(12.5))
                .andExpect(jsonPath("$.retiredAt").doesNotExist())
                .andReturn().getResponse().getHeader("Location");

        UUID playerId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        assertThat(events.stream(PlayerRegistered.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.playerId()).isEqualTo(playerId);
            assertThat(e.initialValue()).isEqualByComparingTo("12.5");
        });
    }

    @Test
    void shirtNumberIsOptional() throws Exception {
        mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Iago", null, "GOALKEEPER", "5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shirtNumber").doesNotExist());
    }

    @Test
    void onlyTheLeagueAdminCanRegister() throws Exception {
        String request = body("Iago", 10, "FORWARD", "10");
        mvc.perform(post("/api/leagues/{id}/players", league).with(as(ana)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        // admin of another league
        mvc.perform(post("/api/leagues/{id}/players", league).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());

        assertThat(playersIn(league)).isZero();
        assertThat(events.stream(PlayerRegistered.class)).isEmpty();
    }

    @Test
    void invalidDataIs400() throws Exception {
        for (String request : List.of(
                body("Iago", 10, "COACH", "10"),
                body("Iago", 10, null, "10"),
                body("Iago", 10, "FORWARD", "0"),
                body("Iago", 10, "FORWARD", "-3"),
                body("Iago", 10, "FORWARD", "1.234"),
                body("Iago", 10, "FORWARD", null),
                body("Iago", 100, "FORWARD", "10"),
                body("Iago", -1, "FORWARD", "10"),
                body("  ", 10, "FORWARD", "10"),
                body("x".repeat(101), 10, "FORWARD", "10"))) {
            mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        }
        assertThat(playersIn(league)).isZero();
    }

    @Test
    void shirtNumberIsUniqueAmongActivePlayersOfTheLeague() throws Exception {
        addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Otro", 10, "DEFENDER", "10")))
                .andExpect(status().isConflict());
        // the same number is free in another league
        mvc.perform(post("/api/leagues/{id}/players", otherLeague).with(as(outsider)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Otro", 10, "DEFENDER", "10")))
                .andExpect(status().isCreated());
        assertThat(playersIn(league)).isEqualTo(1);
    }

    @Test
    void shirtNumberOfARetiredPlayerCanBeReused() throws Exception {
        retire(addPlayer(league, admin, "Iago", 10, Position.FORWARD));

        mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Otro", 10, "DEFENDER", "10")))
                .andExpect(status().isCreated());
    }

    @Test
    void concurrentDuplicateShirtNumberIsStill409() throws Exception {
        addPlayer(league, admin, "Iago", 10, Position.FORWARD);
        // simulate a race: the pre-check passes, the unique index rejects the insert
        doReturn(false).when(players).existsActiveShirtNumber(any(), any());

        mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Otro", 10, "DEFENDER", "10")))
                .andExpect(status().isConflict());
        assertThat(playersIn(league)).isEqualTo(1);
    }

    @Test
    void membersSeeTheActiveSquadOrderedByPositionAndShirtNumber() throws Exception {
        addPlayer(league, admin, "Delantero", 9, Position.FORWARD);
        addPlayer(league, admin, "Medio sin dorsal", null, Position.MIDFIELDER);
        addPlayer(league, admin, "Medio 8", 8, Position.MIDFIELDER);
        addPlayer(league, admin, "Portero", 1, Position.GOALKEEPER);
        addPlayer(league, admin, "Defensa", 4, Position.DEFENDER);
        retire(addPlayer(league, admin, "Retirado", 2, Position.DEFENDER));
        addPlayer(otherLeague, outsider, "De otra liga", 5, Position.DEFENDER);

        mvc.perform(get("/api/leagues/{id}/players", league).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains(
                        "Portero", "Defensa", "Medio 8", "Medio sin dorsal", "Delantero")));
    }

    @Test
    void includeRetiredIsHonouredForTheAdminAndIgnoredForManagers() throws Exception {
        addPlayer(league, admin, "Activo", 9, Position.FORWARD);
        retire(addPlayer(league, admin, "Retirado", 2, Position.DEFENDER));

        mvc.perform(get("/api/leagues/{id}/players", league).param("includeRetired", "true").with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Retirado"))
                .andExpect(jsonPath("$[0].retiredAt").exists());
        mvc.perform(get("/api/leagues/{id}/players", league).with(as(admin)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/leagues/{id}/players", league).param("includeRetired", "true").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Activo"));
    }

    @Test
    void nonMembersCannotListOrSeePlayers() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(get("/api/leagues/{id}/players", league).with(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void membersSeeThePlayerCardIncludingRetiredPlayers() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(player.toString()))
                .andExpect(jsonPath("$.name").value("Iago"))
                .andExpect(jsonPath("$.initialValue").value(10))
                .andExpect(jsonPath("$.currentValue").value(10));

        retire(player);
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retiredAt").exists());
    }

    @Test
    void playersAreIsolatedBetweenLeagues() throws Exception {
        UUID foreign = addPlayer(otherLeague, outsider, "De otra liga", 5, Position.DEFENDER);

        // a member of "Peña" asking for another league's player through "Peña"'s route
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, foreign).with(as(ana)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, UUID.randomUUID()).with(as(ana)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminUpdatesEveryEditableField() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body(" Iago Aspas ", null, "MIDFIELDER", "20.25")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(player.toString()))
                .andExpect(jsonPath("$.name").value("Iago Aspas"))
                .andExpect(jsonPath("$.shirtNumber").doesNotExist())
                .andExpect(jsonPath("$.position").value("MIDFIELDER"))
                .andExpect(jsonPath("$.initialValue").value(20.25));

        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(jsonPath("$.name").value("Iago Aspas"))
                .andExpect(jsonPath("$.currentValue").value(20.25));
    }

    @Test
    void updateKeepsOrChangesTheShirtNumberWithoutClashing() throws Exception {
        UUID iago = addPlayer(league, admin, "Iago", 10, Position.FORWARD);
        addPlayer(league, admin, "Hugo", 9, Position.FORWARD);
        retire(addPlayer(league, admin, "Retirado", 7, Position.DEFENDER));

        // keeping their own number is fine
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, iago).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(body("Iago", 10, "FORWARD", "11")))
                .andExpect(status().isOk());
        // another active player's number is not
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, iago).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(body("Iago", 9, "FORWARD", "11")))
                .andExpect(status().isConflict());
        // a retired player's number is free
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, iago).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Iago", 7, "FORWARD", "11")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shirtNumber").value(7));
    }

    @Test
    void onlyTheLeagueAdminCanUpdate() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);
        String request = body("Cambiado", 10, "FORWARD", "10");

        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(ana)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(outsider)).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        assertThat(nameOf(player)).isEqualTo("Iago");
    }

    @Test
    void invalidUpdateIs400AndChangesNothing() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        for (String request : List.of(
                body("Iago", 10, "COACH", "10"),
                body("Iago", 10, "FORWARD", "0"),
                body("Iago", 100, "FORWARD", "10"),
                body(" ", 10, "FORWARD", "10"))) {
            mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        }
        assertThat(nameOf(player)).isEqualTo("Iago");
    }

    @Test
    void retiredPlayerCannotBeUpdated() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);
        retire(player);

        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content(body("Cambiado", 10, "FORWARD", "10")))
                .andExpect(status().isConflict());
        assertThat(nameOf(player)).isEqualTo("Iago");
    }

    @Test
    void adminRetiresAPlayerWhoLeavesTheActiveSquadButKeepsTheCard() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/leagues/{id}/players", league).with(as(ana)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/leagues/{id}/players", league).param("includeRetired", "true").with(as(admin)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].retiredAt").exists());
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retiredAt").exists());
        assertThat(playersIn(league)).isEqualTo(1);
    }

    @Test
    void retireIsIdempotentAndPublishesPlayerRetiredOnce() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());
        Timestamp firstRetiredAt = retiredAt(player);
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(retiredAt(player)).isEqualTo(firstRetiredAt);
        assertThat(events.stream(PlayerRetired.class)).singleElement().satisfies(e -> {
            assertThat(e.leagueId()).isEqualTo(league);
            assertThat(e.playerId()).isEqualTo(player);
        });
    }

    @Test
    void onlyTheLeagueAdminCanRetire() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);

        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(outsider)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(retiredAt(player)).isNull();
        assertThat(events.stream(PlayerRetired.class)).isEmpty();
    }

    @Test
    void anotherLeaguesPlayerCannotBeUpdatedOrRetired() throws Exception {
        UUID foreign = addPlayer(otherLeague, outsider, "De otra liga", 5, Position.DEFENDER);

        // admin of "Peña" targeting another league's player through "Peña"'s route
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, foreign).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content(body("Robado", 5, "DEFENDER", "10")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, foreign).with(as(admin)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, UUID.randomUUID()).with(as(admin)).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(nameOf(foreign)).isEqualTo("De otra liga");
        assertThat(retiredAt(foreign)).isNull();
    }

    @Test
    void securityWithoutSessionOrCsrf() throws Exception {
        UUID player = addPlayer(league, admin, "Iago", 10, Position.FORWARD);
        String request = body("Otro", 11, "FORWARD", "10");

        mvc.perform(get("/api/leagues/{id}/players", league)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leagues/{id}/players/{p}", league, player)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues/{id}/players", league).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(csrf())
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(csrf()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/leagues/{id}/players", league).with(as(admin))
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(put("/api/leagues/{id}/players/{p}", league, player).with(as(admin))
                .contentType(APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/players/{p}", league, player).with(as(admin)))
                .andExpect(status().isForbidden());
        assertThat(playersIn(league)).isEqualTo(1);
        assertThat(nameOf(player)).isEqualTo("Iago");
        assertThat(retiredAt(player)).isNull();
    }
}
