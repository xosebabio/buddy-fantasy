package com.buddyfantasy.league;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class CreateLeagueTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    LeagueRepository leagues;

    @MockitoSpyBean
    MembershipRepository memberships;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
        jdbc.update("delete from identity_user");
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private static CurrentUser principal(UUID userId) {
        OidcIdToken token = new OidcIdToken("t", NOW, NOW.plusSeconds(3600), Map.of("sub", "any"));
        return new CurrentUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), token, null, userId);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    void createsLeagueWithSettingsRulesAndAdminMembership() throws Exception {
        UUID ana = register("ana");

        mvc.perform(post("/api/leagues").with(oidcLogin().oidcUser(principal(ana))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"  Peña del domingo \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists());

        assertThat(count("league_league")).isEqualTo(1);
        assertThat(count("league_settings")).isEqualTo(1);
        assertThat(count("league_scoring_rule")).isEqualTo(36);
        assertThat(jdbc.queryForObject("select name from league_league", String.class)).isEqualTo("Peña del domingo");

        mvc.perform(get("/api/me/leagues").with(oidcLogin().oidcUser(principal(ana))))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Peña del domingo"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void persistedLeagueRoundTripsWithDefaults() {
        League league = League.create("A", NOW);
        leagues.save(league);

        League loaded = leagues.findById(league.id().value()).orElseThrow();

        assertThat(loaded.scoringRules()).isEqualTo(ScoringRules.defaults());
        LeagueSettings defaults = LeagueSettings.defaults();
        LeagueSettings settings = loaded.settings();
        assertThat(settings.ratingWindowHours()).isEqualTo(defaults.ratingWindowHours());
        assertThat(settings.ratingFormat()).isEqualTo(defaults.ratingFormat());
        assertThat(settings.defaultRating()).isNull();
        assertThat(settings.lineupSize()).isEqualTo(defaults.lineupSize());
        assertThat(settings.lineupBudget()).isEqualByComparingTo(defaults.lineupBudget());
        assertThat(settings.marketK()).isEqualByComparingTo(defaults.marketK());
        assertThat(settings.marketMaxChangePct()).isEqualByComparingTo(defaults.marketMaxChangePct());
        assertThat(settings.marketMinValue()).isEqualByComparingTo(defaults.marketMinValue());
    }

    @Test
    void failureWhileSavingMembershipRollsEverythingBack() {
        UUID ana = register("ana");
        Mockito.doThrow(new IllegalStateException("boom")).when(memberships).save(Mockito.any());

        assertThatThrownBy(() -> bus.dispatch(new CreateLeagueCommand(ana, "A")))
                .hasMessageContaining("boom");

        assertThat(count("league_league")).isZero();
        assertThat(count("league_settings")).isZero();
        assertThat(count("league_scoring_rule")).isZero();
    }

    @Test
    void blankOrTooLongNameIs400() throws Exception {
        UUID ana = register("ana");

        for (String body : List.of("{\"name\":\"  \"}", "{}", "{\"name\":\"" + "x".repeat(101) + "\"}")) {
            mvc.perform(post("/api/leagues").with(oidcLogin().oidcUser(principal(ana))).with(csrf())
                            .contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(count("league_league")).isZero();
    }

    @Test
    void withoutSessionIs401AndWithoutCsrfIs403() throws Exception {
        UUID ana = register("ana");

        mvc.perform(post("/api/leagues").with(csrf()).contentType(APPLICATION_JSON).content("{\"name\":\"A\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues").with(oidcLogin().oidcUser(principal(ana)))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"A\"}"))
                .andExpect(status().isForbidden());
        assertThat(count("league_league")).isZero();
    }
}
