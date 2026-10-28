package com.buddyfantasy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.ApiEndpoints.Endpoint;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.league.LeagueRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * RNF-02: walks every {@code /api/leagues/{id}/**} endpoint registered in the app with a valid session and CSRF token
 * on a real league, so an endpoint that forgets the per-league check fails here. A user outside the league gets 403
 * on all of them and a manager on the admin ones, without anything written.
 *
 * <p>Every endpoint must be in {@link #rules()} with the lowest role allowed and a valid example body (so it does
 * not answer 400 before checking the role): a new endpoint makes {@link #everyLeagueEndpointIsClassified} fail
 * until it is classified.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class LeagueAuthorizationTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    UUID admin;
    UUID manager;
    UUID outsider;
    /** The path variables of the endpoints: the league and a match, player and member of it. */
    Map<String, UUID> variables;

    /** The lowest role allowed: {@link LeagueRole#MANAGER} for any member, since the admin also plays. */
    record Rule(LeagueRole role, String body) {
    }

    @BeforeEach
    void setUp() {
        admin = fixtures.user("admin");
        manager = fixtures.user("manager");
        UUID member = fixtures.user("member");
        outsider = fixtures.user("outsider");
        TestLeague peña = fixtures.league(admin);
        fixtures.join(peña, manager, member);
        // the outsider is ADMIN of another league, never a member of "Peña"
        fixtures.league(outsider, "Otra");
        variables = Map.of(
                "id", peña.id(),
                "matchId", fixtures.schedule(peña, 1, KICKOFF),
                "playerId", fixtures.player(peña, FORWARD),
                "memberId", member);
    }

    private Map<Endpoint, Rule> rules() throws Exception {
        String match = """
                {"matchday": 2, "opponent": "Rival", "venue": "HOME", "kickoffAt": "2026-01-18T10:00:00Z"}""";
        String player = """
                {"name": "Iago", "shirtNumber": 9, "position": "FORWARD", "initialValue": 10}""";
        // the settings of "Peña" as its admin sees them
        String settings = mvc.perform(MockMvcRequestBuilders.get("/api/leagues/{id}/settings", variables.get("id"))
                .with(as(admin))).andReturn().getResponse().getContentAsString();

        Map<Endpoint, Rule> rules = new LinkedHashMap<>();
        // league
        rules.put(endpoint(GET, "/api/leagues/{id}"), member());
        rules.put(endpoint(GET, "/api/leagues/{id}/settings"), member());
        rules.put(endpoint(PUT, "/api/leagues/{id}/settings"), admin(settings));
        rules.put(endpoint(POST, "/api/leagues/{id}/invitation"), admin());
        rules.put(endpoint(DELETE, "/api/leagues/{id}/invitation"), admin());
        rules.put(endpoint(GET, "/api/leagues/{id}/members"), member());
        rules.put(endpoint(DELETE, "/api/leagues/{id}/members/{memberId}"), admin());
        rules.put(endpoint(DELETE, "/api/leagues/{id}/members/me"), member());
        // squad
        rules.put(endpoint(POST, "/api/leagues/{id}/players"), admin(player));
        rules.put(endpoint(GET, "/api/leagues/{id}/players"), member());
        rules.put(endpoint(GET, "/api/leagues/{id}/players/{playerId}"), member());
        rules.put(endpoint(PUT, "/api/leagues/{id}/players/{playerId}"), admin(player));
        rules.put(endpoint(DELETE, "/api/leagues/{id}/players/{playerId}"), admin());
        rules.put(endpoint(POST, "/api/leagues/{id}/players/{playerId}/photo/upload"), admin("""
                {"contentType": "image/webp", "sizeBytes": 30000}"""));
        rules.put(endpoint(PUT, "/api/leagues/{id}/players/{playerId}/photo"), admin("""
                {"photoKey": "players/any.webp"}"""));
        rules.put(endpoint(DELETE, "/api/leagues/{id}/players/{playerId}/photo"), admin());
        // match
        rules.put(endpoint(POST, "/api/leagues/{id}/matches"), admin(match));
        rules.put(endpoint(GET, "/api/leagues/{id}/matches"), member());
        rules.put(endpoint(GET, "/api/leagues/{id}/matches/{matchId}"), member());
        rules.put(endpoint(PUT, "/api/leagues/{id}/matches/{matchId}"), admin(match));
        rules.put(endpoint(DELETE, "/api/leagues/{id}/matches/{matchId}"), admin());
        rules.put(endpoint(PUT, "/api/leagues/{id}/matches/{matchId}/sheet"), admin("""
                {"result": null, "performances": []}"""));
        rules.put(endpoint(POST, "/api/leagues/{id}/matches/{matchId}/close"), admin());
        rules.put(endpoint(GET, "/api/leagues/{id}/matches/{matchId}/audit"), admin());
        // lineup
        rules.put(endpoint(GET, "/api/leagues/{id}/lineup"), member());
        rules.put(endpoint(PUT, "/api/leagues/{id}/lineup"), member("""
                {"playerIds": []}"""));
        rules.put(endpoint(GET, "/api/leagues/{id}/lineups/{matchId}"), member());
        return rules;
    }

    private static Endpoint endpoint(HttpMethod method, String pattern) {
        return new Endpoint(method, pattern);
    }

    private static Rule member() {
        return new Rule(LeagueRole.MANAGER, null);
    }

    private static Rule member(String body) {
        return new Rule(LeagueRole.MANAGER, body);
    }

    private static Rule admin() {
        return new Rule(LeagueRole.ADMIN, null);
    }

    private static Rule admin(String body) {
        return new Rule(LeagueRole.ADMIN, body);
    }

    private List<Endpoint> leagueEndpoints() {
        return ApiEndpoints.scan(mappings).stream()
                .filter(endpoint -> endpoint.pattern().startsWith("/api/leagues/{id}"))
                .toList();
    }

    /** Calls each endpoint as {@code user} and returns those that did not answer 403, with their status. */
    private Map<Endpoint, Integer> notForbidden(UUID user, Map<Endpoint, Rule> rules) throws Exception {
        Map<Endpoint, Integer> answered = new LinkedHashMap<>();
        for (Map.Entry<Endpoint, Rule> entry : rules.entrySet()) {
            Endpoint endpoint = entry.getKey();
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                    .request(endpoint.method(), endpoint.path(variables)).with(as(user)).with(csrf());
            if (entry.getValue().body() != null) {
                request.contentType(APPLICATION_JSON).content(entry.getValue().body());
            }
            int status = mvc.perform(request).andReturn().getResponse().getStatus();
            if (status != 403) {
                answered.put(endpoint, status);
            }
        }
        return answered;
    }

    /** To check that the rejected requests wrote nothing. */
    private Map<String, String> snapshot() {
        return TableDigests.of(jdbc, TableDigests.ALWAYS_CHANGING);
    }

    @Test
    @DisplayName("Every league endpoint is classified with its minimum role")
    void everyLeagueEndpointIsClassified() throws Exception {
        assertThat(rules().keySet())
                .as("every /api/leagues/{id}/** endpoint needs a rule in LeagueAuthorizationTests.rules()")
                .containsExactlyInAnyOrderElementsOf(leagueEndpoints());
    }

    @Test
    @DisplayName("A user outside the league gets 403 on every league endpoint")
    void aUserOutsideTheLeagueGets403OnEveryLeagueEndpoint() throws Exception {
        Map<Endpoint, Rule> rules = rules();
        Map<String, String> before = snapshot();

        assertThat(notForbidden(outsider, rules)).isEmpty();
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    @DisplayName("A manager gets 403 on every admin endpoint")
    void aManagerGets403OnEveryAdminEndpoint() throws Exception {
        Map<Endpoint, Rule> adminOnly = new LinkedHashMap<>(rules());
        adminOnly.values().removeIf(rule -> rule.role() != LeagueRole.ADMIN);
        Map<String, String> before = snapshot();

        assertThat(notForbidden(manager, adminOnly)).isEmpty();
        assertThat(snapshot()).isEqualTo(before);
    }
}
