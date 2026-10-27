package com.buddyfantasy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class MeEndpointsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    LeagueRepository leagues;

    @Autowired
    LeagueDefaultsRepository defaults;

    @Autowired
    LeagueLimitsRepository limits;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_invitation");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
        jdbc.update("delete from identity_user");
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, "http://pic/" + sub)).value();
    }

    private static CurrentUser principal(UUID userId) {
        OidcIdToken token = new OidcIdToken("t", NOW, NOW.plusSeconds(3600), Map.of("sub", "any"));
        return new CurrentUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token, null, userId);
    }

    private void join(String leagueName, UUID userId, LeagueRole role) {
        League league = League.create(leagueName, NOW, defaults.settings(), defaults.scoringRules(), limits.get());
        leagues.save(league);
        memberships.save(Membership.join(league.id(), userId, role, NOW));
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        UUID id = register("ana");

        mvc.perform(get("/api/me").with(oidcLogin().oidcUser(principal(id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.displayName").value("Name ana"))
                .andExpect(jsonPath("$.email").value("ana@x.com"))
                .andExpect(jsonPath("$.avatarUrl").value("http://pic/ana"));
    }

    @Test
    void meWithoutSessionIs401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void myLeaguesReturnsOnlyTheUsersLeaguesWithTheirRole() throws Exception {
        UUID ana = register("ana");
        UUID bob = register("bob");
        join("Alpha", ana, LeagueRole.ADMIN);
        join("Beta", ana, LeagueRole.MANAGER);
        join("Gamma", bob, LeagueRole.ADMIN);

        mvc.perform(get("/api/me/leagues").with(oidcLogin().oidcUser(principal(ana))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].name").value("Beta"))
                .andExpect(jsonPath("$[1].role").value("MANAGER"));
    }

    @Test
    void myLeaguesIsEmptyWithoutMemberships() throws Exception {
        UUID ana = register("ana");

        mvc.perform(get("/api/me/leagues").with(oidcLogin().oidcUser(principal(ana))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
