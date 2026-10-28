package com.buddyfantasy.league;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    TestFixtures fixtures;

    @Autowired
    LeagueRepository leagues;

    @Autowired
    LeagueDefaultsRepository defaults;

    @Autowired
    LeagueLimitsRepository limits;

    @MockitoSpyBean
    MembershipRepository memberships;

    @Autowired
    JdbcTemplate jdbc;

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    @Test
    @DisplayName("Creates the league with its settings, scoring rules and the admin membership")
    void createsLeagueWithSettingsRulesAndAdminMembership() throws Exception {
        UUID ana = fixtures.user("ana");

        mvc.perform(post("/api/leagues").with(as(ana)).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"  Peña del domingo \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.invitationToken").isNotEmpty());

        assertThat(count("league_league")).isEqualTo(1);
        assertThat(count("league_settings")).isEqualTo(1);
        assertThat(count("league_scoring_rule")).isEqualTo(36);
        assertThat(count("league_invitation")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select name from league_league", String.class)).isEqualTo("Peña del domingo");

        mvc.perform(get("/api/me/leagues").with(as(ana)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Peña del domingo"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    @DisplayName("A persisted league round-trips with its defaults")
    void persistedLeagueRoundTripsWithDefaults() {
        League league = League.create("A", NOW, defaults.settings(), defaults.scoringRules(), limits.get());
        leagues.save(league);

        League loaded = leagues.findById(league.id().value()).orElseThrow();

        assertThat(loaded.scoringRules()).isEqualTo(defaults.scoringRules());
        LeagueSettings expected = defaults.settings();
        LeagueSettings settings = loaded.settings();
        assertThat(settings).usingRecursiveComparison().withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("A failure while saving the membership rolls everything back")
    void failureWhileSavingMembershipRollsEverythingBack() {
        UUID ana = fixtures.user("ana");
        Mockito.doThrow(new IllegalStateException("boom")).when(memberships).save(Mockito.any());

        assertThatThrownBy(() -> bus.dispatch(new CreateLeagueCommand(ana, "A")))
                .hasMessageContaining("boom");

        assertThat(count("league_league")).isZero();
        assertThat(count("league_settings")).isZero();
        assertThat(count("league_scoring_rule")).isZero();
    }

    @Test
    @DisplayName("A blank or too long name is answered with 400")
    void blankOrTooLongNameIs400() throws Exception {
        UUID ana = fixtures.user("ana");

        for (String body : List.of("{\"name\":\"  \"}", "{}")) {
            mvc.perform(post("/api/leagues").with(as(ana)).with(csrf())
                            .contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/leagues").with(as(ana)).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LEAGUE_NAME"));
        assertThat(count("league_league")).isZero();
    }

    @Test
    @DisplayName("Without a session it is 401 and without CSRF it is 403")
    void withoutSessionIs401AndWithoutCsrfIs403() throws Exception {
        UUID ana = fixtures.user("ana");

        mvc.perform(post("/api/leagues").with(csrf()).contentType(APPLICATION_JSON).content("{\"name\":\"A\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/leagues").with(as(ana))
                        .contentType(APPLICATION_JSON).content("{\"name\":\"A\"}"))
                .andExpect(status().isForbidden());
        assertThat(count("league_league")).isZero();
    }
}
