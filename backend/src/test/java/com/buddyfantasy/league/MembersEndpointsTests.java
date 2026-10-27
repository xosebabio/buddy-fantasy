package com.buddyfantasy.league;

import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class MembersEndpointsTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    UUID admin;
    UUID ana;
    UUID bob;
    UUID outsider;
    UUID league;

    @BeforeEach
    void setUp() {
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        bob = fixtures.user("bob");
        outsider = fixtures.user("outsider");
        TestLeague peña = fixtures.league(admin);
        league = peña.id();
        fixtures.join(peña, ana, bob);
    }

    private int members() {
        return jdbc.queryForObject("select count(*) from league_membership where league_id = ?", Integer.class, league);
    }

    @Test
    void membersSeeEveryoneIncludingTheAdminWithoutEmails() throws Exception {
        mvc.perform(get("/api/leagues/{id}/members", league).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].userId").value(admin.toString()))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].displayName").value("Name admin"))
                .andExpect(jsonPath("$[0].avatarUrl").value("http://pic/admin"))
                .andExpect(jsonPath("$[1].displayName").value("Name ana"))
                .andExpect(jsonPath("$[1].role").value("MANAGER"))
                .andExpect(jsonPath("$[*].email").doesNotExist());
    }

    @Test
    void nonMemberCannotListMembers() throws Exception {
        mvc.perform(get("/api/leagues/{id}/members", league).with(as(outsider))).andExpect(status().isForbidden());
    }

    @Test
    void adminRemovesAManagerWhoLosesAccess() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, ana).with(as(admin)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(members()).isEqualTo(2);
        mvc.perform(get("/api/leagues/{id}", league).with(as(ana))).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/leagues").with(as(ana))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removingANonMemberIs404() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, outsider).with(as(admin)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void managersAndOutsidersCannotRemove() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, bob).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, bob).with(as(outsider)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(members()).isEqualTo(3);
    }

    @Test
    void adminCannotRemoveThemselfNorLeave() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, admin).with(as(admin)).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(as(admin)).with(csrf()))
                .andExpect(status().isConflict());
        assertThat(members()).isEqualTo(3);
    }

    @Test
    void managerLeavesAndLosesAccess() throws Exception {
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(as(ana)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(members()).isEqualTo(2);
        mvc.perform(get("/api/leagues/{id}", league).with(as(ana))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(as(outsider)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesArePerLeague() throws Exception {
        // ana is MANAGER in "Peña" and ADMIN in her own league
        TestLeague anaLeague = fixtures.league(ana, "De Ana");
        fixtures.join(anaLeague, bob);

        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, bob).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/{m}", anaLeague.id(), bob).with(as(ana)).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(members()).isEqualTo(3);
    }

    @Test
    void securityWithoutSessionOrCsrf() throws Exception {
        mvc.perform(get("/api/leagues/{id}/members", league)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, ana).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(csrf())).andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, ana).with(as(admin))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/me", league).with(as(ana))).andExpect(status().isForbidden());
        assertThat(members()).isEqualTo(3);
    }
}
