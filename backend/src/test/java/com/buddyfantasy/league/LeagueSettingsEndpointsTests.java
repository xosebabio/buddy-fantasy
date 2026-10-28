package com.buddyfantasy.league;

import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class LeagueSettingsEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    UUID admin;
    UUID manager;
    UUID outsider;
    UUID league;

    @BeforeEach
    void setUp() {
        admin = fixtures.user("admin");
        manager = fixtures.user("manager");
        outsider = fixtures.user("outsider");
        TestLeague peña = fixtures.league(admin);
        league = peña.id();
        fixtures.join(peña, manager);
    }

    private ObjectNode currentSettings() throws Exception {
        String body = mvc.perform(get("/api/leagues/{id}/settings", league).with(as(admin)))
                .andReturn().getResponse().getContentAsString();
        return (ObjectNode) json.readTree(body);
    }

    private org.springframework.test.web.servlet.ResultActions update(UUID user, ObjectNode body) throws Exception {
        return mvc.perform(put("/api/leagues/{id}/settings", league).with(as(user)).with(csrf())
                .contentType(APPLICATION_JSON).content(body.toString()));
    }

    @Test
    @DisplayName("Members see the league with their own role")
    void membersSeeLeagueWithTheirOwnRole() throws Exception {
        mvc.perform(get("/api/leagues/{id}", league).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Peña"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/leagues/{id}", league).with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("A non-member cannot read the league or its settings")
    void nonMemberCannotReadLeagueOrSettings() throws Exception {
        mvc.perform(get("/api/leagues/{id}", league).with(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/leagues/{id}/settings", league).with(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/leagues/{id}", UUID.randomUUID()).with(as(outsider))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A manager reads the default settings and scoring table")
    void managerReadsDefaultSettingsAndScoringTable() throws Exception {
        mvc.perform(get("/api/leagues/{id}/settings", league).with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingWindowHours").value(24))
                .andExpect(jsonPath("$.ratingFormat").value("SPADES"))
                .andExpect(jsonPath("$.ratingSpadesFactor").value(4))
                .andExpect(jsonPath("$.ratingSpadesBase").value(-2))
                .andExpect(jsonPath("$.ratingDecimalFactor").value(1))
                .andExpect(jsonPath("$.ratingDecimalBase").value(0))
                .andExpect(jsonPath("$.ratingMinVotes").value(1))
                .andExpect(jsonPath("$.ratingPointsPerUnit").doesNotExist())
                .andExpect(jsonPath("$.lineupSize").value(7))
                .andExpect(jsonPath("$.lineupLockHours").value(2))
                .andExpect(jsonPath("$.lineupFormationEnabled").value(false))
                .andExpect(jsonPath("$.lineupMinGoalkeepers").value(1))
                .andExpect(jsonPath("$.lineupMinDefenders").value(1))
                .andExpect(jsonPath("$.lineupMinMidfielders").value(1))
                .andExpect(jsonPath("$.lineupMinForwards").value(1))
                .andExpect(jsonPath("$.marketK").value(0.05))
                .andExpect(jsonPath("$.scoringRules.GOAL.GOALKEEPER").value(6))
                .andExpect(jsonPath("$.scoringRules.PENALTY_GOAL.FORWARD").value(3))
                .andExpect(jsonPath("$.scoringRules.DOUBLE_YELLOW.DEFENDER").value(-3));
    }

    @Test
    @DisplayName("The admin replaces the settings and scoring rules")
    void adminReplacesSettingsAndScoringRules() throws Exception {
        ObjectNode body = currentSettings();
        body.put("lineupSize", 9).put("ratingFormat", "DECIMAL").put("defaultRating", 5)
                .put("ratingDecimalFactor", 1.5).put("ratingDecimalBase", -1).put("ratingMinVotes", 3)
                .put("lineupBudgetEnabled", true).put("marketK", 0.1);
        ((ObjectNode) body.get("scoringRules").get("GOAL")).put("FORWARD", 4);

        update(admin, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.lineupSize").value(9))
                .andExpect(jsonPath("$.scoringRules.GOAL.FORWARD").value(4));

        mvc.perform(get("/api/leagues/{id}/settings", league).with(as(manager)))
                .andExpect(jsonPath("$.lineupSize").value(9))
                .andExpect(jsonPath("$.ratingFormat").value("DECIMAL"))
                .andExpect(jsonPath("$.defaultRating").value(5))
                .andExpect(jsonPath("$.ratingDecimalFactor").value(1.5))
                .andExpect(jsonPath("$.ratingDecimalBase").value(-1))
                .andExpect(jsonPath("$.ratingSpadesFactor").value(4))
                .andExpect(jsonPath("$.ratingMinVotes").value(3))
                .andExpect(jsonPath("$.lineupBudgetEnabled").value(true))
                .andExpect(jsonPath("$.marketK").value(0.1))
                .andExpect(jsonPath("$.scoringRules.GOAL.FORWARD").value(4))
                .andExpect(jsonPath("$.scoringRules.GOAL.GOALKEEPER").value(6));
        assertThat(jdbc.queryForObject("select count(*) from league_scoring_rule", Integer.class)).isEqualTo(36);
    }

    @Test
    @DisplayName("The admin configures the formation minimums and the lock hours")
    void adminConfiguresFormationMinimumsAndLockHours() throws Exception {
        ObjectNode body = currentSettings().put("lineupFormationEnabled", true).put("lineupMinGoalkeepers", 1)
                .put("lineupMinDefenders", 3).put("lineupMinMidfielders", 2).put("lineupMinForwards", 0)
                .put("lineupLockHours", 168);

        update(admin, body).andExpect(status().isOk());

        mvc.perform(get("/api/leagues/{id}/settings", league).with(as(manager)))
                .andExpect(jsonPath("$.lineupFormationEnabled").value(true))
                .andExpect(jsonPath("$.lineupMinGoalkeepers").value(1))
                .andExpect(jsonPath("$.lineupMinDefenders").value(3))
                .andExpect(jsonPath("$.lineupMinMidfielders").value(2))
                .andExpect(jsonPath("$.lineupMinForwards").value(0))
                .andExpect(jsonPath("$.lineupLockHours").value(168));
    }

    @Test
    @DisplayName("A manager and the admin of another league cannot edit")
    void managerAndAdminOfAnotherLeagueCannotEdit() throws Exception {
        UUID otherLeague = fixtures.league(outsider, "Otra").id();
        ObjectNode body = currentSettings().put("lineupSize", 11);

        update(manager, body).andExpect(status().isForbidden());
        update(outsider, body).andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("select lineup_size from league_settings where league_id = ?",
                Integer.class, league)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select lineup_size from league_settings where league_id = ?",
                Integer.class, otherLeague)).isEqualTo(7);
    }

    @Test
    @DisplayName("Invalid values are answered with 400 and nothing changes")
    void invalidValuesAre400AndNothingChanges() throws Exception {
        update(admin, currentSettings().put("lineupSize", 0)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("marketMaxChangePct", 101)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("defaultRating", 5)).andExpect(status().isBadRequest()); // SPADES max is 4
        update(admin, currentSettings().put("ratingFormat", "BOGUS")).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("ratingSpadesFactor", 0)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("ratingDecimalFactor", 100000)).andExpect(status().isBadRequest());
        update(admin, currentSettings().putNull("ratingSpadesBase")).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("ratingMinVotes", 0)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("lineupLockHours", 169)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("lineupMinDefenders", -1)).andExpect(status().isBadRequest());
        update(admin, currentSettings().put("lineupFormationEnabled", true).put("lineupMinMidfielders", 5))
                .andExpect(status().isBadRequest()); // 1 + 1 + 5 + 1 > 7
        update(admin, currentSettings().putNull("ratingFormat")).andExpect(status().isBadRequest());
        ObjectNode incomplete = currentSettings();
        ((ObjectNode) incomplete.get("scoringRules")).remove("RED_CARD");
        update(admin, incomplete).andExpect(status().isBadRequest());
        ObjectNode noRules = currentSettings();
        noRules.remove("scoringRules");
        update(admin, noRules).andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select lineup_size from league_settings", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("Requires a session and a CSRF token")
    void securityWithoutSessionOrCsrf() throws Exception {
        mvc.perform(get("/api/leagues/{id}", league)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leagues/{id}/settings", league)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/settings", league).with(csrf())
                .contentType(APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/leagues/{id}/settings", league).with(as(admin))
                .contentType(APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }
}
