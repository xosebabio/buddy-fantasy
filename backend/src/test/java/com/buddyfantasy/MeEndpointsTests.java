package com.buddyfantasy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class MeEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Test
    @DisplayName("/me returns the authenticated user")
    void meReturnsTheAuthenticatedUser() throws Exception {
        UUID id = fixtures.user("ana");

        mvc.perform(get("/api/me").with(as(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.displayName").value("Name ana"))
                .andExpect(jsonPath("$.email").value("ana@x.com"))
                .andExpect(jsonPath("$.avatarUrl").value("http://pic/ana"));
    }

    @Test
    @DisplayName("/me without a session is answered with 401")
    void meWithoutSessionIs401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("My leagues returns only the user's leagues with their role")
    void myLeaguesReturnsOnlyTheUsersLeaguesWithTheirRole() throws Exception {
        UUID ana = fixtures.user("ana");
        UUID bob = fixtures.user("bob");
        fixtures.league(ana, "Alpha");
        fixtures.join(fixtures.league(bob, "Beta"), ana);
        fixtures.league(bob, "Gamma");

        mvc.perform(get("/api/me/leagues").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].name").value("Beta"))
                .andExpect(jsonPath("$[1].role").value("MANAGER"));
    }

    @Test
    @DisplayName("My leagues is empty without memberships")
    void myLeaguesIsEmptyWithoutMemberships() throws Exception {
        UUID ana = fixtures.user("ana");

        mvc.perform(get("/api/me/leagues").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
