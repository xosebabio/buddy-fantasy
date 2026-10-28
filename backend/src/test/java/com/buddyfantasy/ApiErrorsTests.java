package com.buddyfantasy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.ApiEndpoints.Endpoint;
import com.buddyfantasy.TestFixtures.TestLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP error contract: a malformed request is a 4xx {@code ProblemDetail}, never a 500, and an unexpected error
 * is a 500 that tells the caller nothing about its cause. The scans walk every endpoint registered in the app, so
 * the endpoints added later are covered without listing them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class ApiErrorsTests {

    static final Instant KICKOFF = Instant.parse("2026-01-11T10:00:00Z");
    static final String INTERNAL_MESSAGE = "connection to db-replica-7:5432 refused";

    private static final Set<HttpMethod> MUTATING = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,
            HttpMethod.PATCH);

    /** Bodies that no endpoint can accept: none, empty, empty object, JSON null, wrong JSON type, unreadable. */
    private static final List<String> BAD_BODIES = List.of("", "{}", "null", "[]", "{");

    /** Fails on every call, as a bug in a handler would. */
    @RestController
    static class FailingController {

        @GetMapping("/api/test/failure")
        String fail() {
            throw new IllegalStateException(INTERNAL_MESSAGE);
        }
    }

    @TestConfiguration
    static class FailingControllerConfig {

        @Bean
        FailingController failingController() {
            return new FailingController();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    TestLeague peña;
    /** The path variables of the endpoints: a league the user is admin of, and a match, player and member of it. */
    Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        peña = fixtures.league(fixtures.user("admin"));
        UUID member = fixtures.user("member");
        fixtures.join(peña, member);
        variables = Map.of(
                "id", peña.id(),
                "matchId", fixtures.schedule(peña, 1, KICKOFF),
                "playerId", fixtures.player(peña, FORWARD),
                "memberId", member,
                "token", peña.invitationToken());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(as(peña.admin())).with(csrf()));
    }

    private static void isProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    @DisplayName("Malformed JSON is answered with 400")
    void malformedJsonIs400() throws Exception {
        isProblem(perform(post("/api/leagues/{id}/players", peña.id())
                .contentType(APPLICATION_JSON).content("{\"name\": \"Iago\",")), 400);
    }

    @Test
    @DisplayName("A missing body is answered with 400")
    void aMissingBodyIs400() throws Exception {
        isProblem(perform(post("/api/leagues/{id}/players", peña.id()).contentType(APPLICATION_JSON)), 400);
    }

    @Test
    @DisplayName("An unknown enum value is answered with 400 and nothing is registered")
    void anUnknownEnumValueIs400AndRegistersNothing() throws Exception {
        isProblem(perform(post("/api/leagues/{id}/players", peña.id()).contentType(APPLICATION_JSON).content("""
                {"name": "Iago", "shirtNumber": 9, "position": "PORTERO", "initialValue": 10}""")), 400);

        perform(get("/api/leagues/{id}/players", peña.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // only the player of setUp
    }

    @Test
    @DisplayName("An invalid UUID in the path is answered with 400")
    void anInvalidUuidInThePathIs400() throws Exception {
        isProblem(perform(get("/api/leagues/{id}/players", "not-a-uuid")), 400);
        isProblem(perform(get("/api/leagues/{id}/matches/{matchId}", peña.id(), "42")), 400);
        isProblem(perform(get("/api/leagues/{id}/standings/{matchId}", peña.id(), "42")), 400);
        isProblem(perform(get("/api/leagues/{id}/managers/{userId}/history", peña.id(), "42")), 400);
        isProblem(perform(get("/api/leagues/{id}/players/{playerId}/points", peña.id(), "42")), 400);
        isProblem(perform(get("/api/leagues/{id}/ratings/{matchId}", peña.id(), "42")), 400);
    }

    @Test
    @DisplayName("An unsupported HTTP method is answered with 405")
    void anUnsupportedMethodIs405() throws Exception {
        isProblem(perform(patch("/api/leagues/{id}", peña.id())), 405);
    }

    @Test
    @DisplayName("An access denied is answered with 403")
    void anAccessDeniedIs403() throws Exception {
        UUID outsider = fixtures.user("outsider");

        isProblem(mvc.perform(get("/api/leagues/{id}/players", peña.id()).with(as(outsider))), 403);
    }

    @Test
    @DisplayName("An unexpected error is answered with 500 without exposing its cause")
    void anUnexpectedErrorIs500WithoutItsCause() throws Exception {
        String body = perform(get("/api/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Internal error"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(INTERNAL_MESSAGE, "IllegalStateException", "FailingController", "trace");
    }

    @Test
    @DisplayName("The scan finds the endpoints that take a body")
    void scanFindsTheEndpointsWithABody() {
        assertThat(ApiEndpoints.withBody(mappings))
                .hasSizeGreaterThanOrEqualTo(10)
                .contains(
                        new Endpoint(HttpMethod.POST, "/api/leagues"),
                        new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/settings"),
                        new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/matches/{matchId}/sheet"),
                        new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/lineup"),
                        new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/ratings/{matchId}"))
                .doesNotContain(
                        new Endpoint(HttpMethod.POST, "/api/leagues/{id}/matches/{matchId}/close"),
                        new Endpoint(HttpMethod.DELETE, "/api/leagues/{id}/players/{playerId}"));
    }

    @Test
    @DisplayName("Every endpoint with a body answers an invalid body with 400")
    void everyEndpointWithABodyAnswersABadBodyWith400() throws Exception {
        Map<String, Integer> notBadRequest = new LinkedHashMap<>();
        for (Endpoint endpoint : ApiEndpoints.withBody(mappings)) {
            for (String body : BAD_BODIES) {
                var response = perform(MockMvcRequestBuilders.request(endpoint.method(), endpoint.path(variables))
                        .contentType(APPLICATION_JSON).content(body)).andReturn().getResponse();
                String type = response.getContentType();
                if (response.getStatus() != 400 || type == null
                        || !APPLICATION_PROBLEM_JSON.isCompatibleWith(MediaType.parseMediaType(type))) {
                    notBadRequest.put(endpoint + " with '" + body + "'", response.getStatus());
                }
            }
        }

        assertThat(notBadRequest).isEmpty();
    }

    @Test
    @DisplayName("No mutating endpoint answers an empty body with 500")
    void noMutatingEndpointAnswersAnEmptyBodyWith500() throws Exception {
        Map<Endpoint, Integer> serverErrors = new LinkedHashMap<>();
        for (Endpoint endpoint : ApiEndpoints.scan(mappings)) {
            if (MUTATING.contains(endpoint.method())) {
                int status = perform(MockMvcRequestBuilders.request(endpoint.method(), endpoint.path(variables)))
                        .andReturn().getResponse().getStatus();
                if (status >= 500) {
                    serverErrors.put(endpoint, status);
                }
            }
        }

        assertThat(serverErrors).isEmpty();
    }
}
