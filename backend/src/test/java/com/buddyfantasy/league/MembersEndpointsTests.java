package com.buddyfantasy.league;

import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.league.application.usecases.regenerateinvitation.RegenerateInvitationCommand;
import com.buddyfantasy.shared.application.CommandBus;
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
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    UUID admin;
    UUID ana;
    UUID bob;
    UUID outsider;
    UUID league;

    @BeforeEach
    void setUp() {
        admin = register("admin");
        ana = register("ana");
        bob = register("bob");
        outsider = register("outsider");
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, "Peña"));
        league = created.id().value();
        bus.dispatch(new JoinLeagueCommand(ana, created.invitationToken()));
        bus.dispatch(new JoinLeagueCommand(bob, created.invitationToken()));
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, "http://pic/" + sub)).value();
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
        UUID anaLeague = bus.dispatch(new CreateLeagueCommand(ana, "De Ana")).id().value();
        bus.dispatch(new JoinLeagueCommand(bob, inviteTo(anaLeague)));

        mvc.perform(delete("/api/leagues/{id}/members/{m}", league, bob).with(as(ana)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/leagues/{id}/members/{m}", anaLeague, bob).with(as(ana)).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(members()).isEqualTo(3);
    }

    private String inviteTo(UUID leagueId) {
        UUID leagueAdmin = jdbc.queryForObject(
                "select user_id from league_membership where league_id = ? and role = 'ADMIN'", UUID.class, leagueId);
        return bus.dispatch(new RegenerateInvitationCommand(leagueAdmin, leagueId));
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
