package com.buddyfantasy.squad;

import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Values different from {@code application.yml}, so the test proves the endpoint reads the configuration. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
@TestPropertySource(properties = {
        "squad.limits.name-max-length=40",
        "squad.limits.shirt-number-min=1",
        "squad.limits.shirt-number-max=50"
})
class SquadLimitsEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

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

    @Test
    @DisplayName("The admin gets the configured limits")
    void adminGetsConfiguredLimits() throws Exception {
        mvc.perform(get("/api/leagues/{id}/players/limits", league).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameMaxLength").value(40))
                .andExpect(jsonPath("$.shirtNumberMin").value(1))
                .andExpect(jsonPath("$.shirtNumberMax").value(50));
    }

    @Test
    @DisplayName("A manager cannot read the limits")
    void managerCannotReadLimits() throws Exception {
        mvc.perform(get("/api/leagues/{id}/players/limits", league).with(as(manager))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A non-member cannot read the limits")
    void nonMemberCannotReadLimits() throws Exception {
        mvc.perform(get("/api/leagues/{id}/players/limits", league).with(as(outsider))).andExpect(status().isForbidden());
    }
}
