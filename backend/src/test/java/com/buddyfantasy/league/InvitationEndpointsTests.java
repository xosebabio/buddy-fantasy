package com.buddyfantasy.league;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class InvitationEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    UUID admin;
    UUID ana;
    UUID bob;
    UUID league;
    String token;

    @BeforeEach
    void setUp() {
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        bob = fixtures.user("bob");
        TestLeague peña = fixtures.league(admin);
        league = peña.id();
        token = peña.invitationToken();
    }

    private ResultActions join(UUID user, String t) throws Exception {
        return mvc.perform(post("/api/invitations/{t}/join", t).with(as(user)).with(csrf()));
    }

    private ResultActions preview(UUID user, String t) throws Exception {
        return mvc.perform(get("/api/invitations/{t}", t).with(as(user)));
    }

    private String regenerate(UUID user) throws Exception {
        String body = mvc.perform(post("/api/leagues/{id}/invitation", league).with(as(user)).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private int memberships() {
        return jdbc.queryForObject("select count(*) from league_membership where league_id = ?", Integer.class, league);
    }

    @Test
    void joiningWithTheInitialTokenCreatesAManager() throws Exception {
        join(ana, token).andExpect(status().isOk()).andExpect(jsonPath("$.leagueId").value(league.toString()));

        assertThat(jdbc.queryForObject("select role from league_membership where user_id = ?", String.class, ana))
                .isEqualTo("MANAGER");
        mvc.perform(get("/api/me/leagues").with(as(ana)))
                .andExpect(jsonPath("$[0].name").value("Peña"))
                .andExpect(jsonPath("$[0].role").value("MANAGER"));
    }

    @Test
    void joiningTwiceDoesNotDuplicateAndAdminKeepsTheirRole() throws Exception {
        join(ana, token).andExpect(status().isOk());
        join(ana, token).andExpect(status().isOk());
        join(admin, token).andExpect(status().isOk());

        assertThat(memberships()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select role from league_membership where user_id = ?", String.class, admin))
                .isEqualTo("ADMIN");
    }

    @Test
    void previewShowsOnlyNameAndMembership() throws Exception {
        preview(ana, token).andExpect(status().isOk())
                .andExpect(jsonPath("$.leagueName").value("Peña"))
                .andExpect(jsonPath("$.alreadyMember").value(false))
                .andExpect(jsonPath("$.length()").value(2));
        preview(admin, token).andExpect(jsonPath("$.alreadyMember").value(true));
    }

    @Test
    void regeneratingInvalidatesThePreviousTokenAndKeepsOneActive() throws Exception {
        String fresh = regenerate(admin);

        assertThat(fresh).isNotEqualTo(token);
        preview(ana, token).andExpect(status().isNotFound());
        join(ana, token).andExpect(status().isNotFound());
        join(ana, fresh).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from league_invitation where revoked_at is null",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from league_invitation", Integer.class)).isEqualTo(2);
    }

    @Test
    void revokedTokenIs404AndRevokingTwiceIsFine() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/invitation", league).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/leagues/{id}/invitation", league).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        preview(ana, token).andExpect(status().isNotFound());
        join(ana, token).andExpect(status().isNotFound());
        assertThat(memberships()).isEqualTo(1);

        // a new link can be issued afterwards
        join(ana, regenerate(admin)).andExpect(status().isOk());
    }

    @Test
    void unknownTokenIs404WithAGenericBody() throws Exception {
        preview(ana, "nope").andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Not found"));
        join(ana, "nope").andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAdminOfTheLeagueManagesTheInvitation() throws Exception {
        join(ana, token).andExpect(status().isOk());
        UUID otherLeague = fixtures.league(bob, "Otra").id();

        for (UUID user : List.of(ana, bob)) {
            mvc.perform(post("/api/leagues/{id}/invitation", league).with(as(user)).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/leagues/{id}/invitation", league).with(as(user)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        assertThat(otherLeague).isNotEqualTo(league);
        preview(bob, token).andExpect(status().isOk()); // original link untouched
    }

    @Test
    void onlyTheTokenHashIsPersisted() {
        String stored = jdbc.queryForObject("select token_hash from league_invitation", String.class);

        assertThat(stored).isNotEqualTo(token).matches("[0-9a-f]{64}");
        assertThat(jdbc.queryForObject("select count(*) from league_invitation where token_hash = ?",
                Integer.class, token)).isZero();
    }

    @Test
    void securityWithoutSessionOrCsrf() throws Exception {
        mvc.perform(get("/api/invitations/{t}", token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/invitations/{t}/join", token).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues/{id}/invitation", league).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/invitation", league).with(csrf())).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/invitations/{t}/join", token).with(as(ana))).andExpect(status().isForbidden());
        mvc.perform(post("/api/leagues/{id}/invitation", league).with(as(admin))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/invitation", league).with(as(admin))).andExpect(status().isForbidden());
        assertThat(memberships()).isEqualTo(1);
    }
}
