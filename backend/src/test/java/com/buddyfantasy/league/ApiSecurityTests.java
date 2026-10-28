package com.buddyfantasy.league;

import java.util.List;
import java.util.Set;

import com.buddyfantasy.ApiEndpoints;
import com.buddyfantasy.ApiEndpoints.Endpoint;
import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks every {@code /api/**} mapping registered in the app, so endpoints added later are covered automatically:
 * no session → 401, mutation with a session but no CSRF token → 403. The role and league checks are scanned by
 * {@link com.buddyfantasy.LeagueAuthorizationTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class ApiSecurityTests {

    private static final Set<HttpMethod> MUTATING = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH);

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    private List<Endpoint> apiEndpoints() {
        return ApiEndpoints.scan(mappings);
    }

    @Test
    @DisplayName("The scan finds all API endpoints")
    void scanFindsAllApiEndpoints() {
        assertThat(apiEndpoints()).hasSizeGreaterThanOrEqualTo(24);
    }

    @Test
    @DisplayName("The scan covers the match endpoints")
    void scanCoversTheMatchEndpoints() {
        assertThat(apiEndpoints()).contains(
                new Endpoint(HttpMethod.POST, "/api/leagues/{id}/matches"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/matches"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/matches/{matchId}"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/matches/{matchId}"),
                new Endpoint(HttpMethod.DELETE, "/api/leagues/{id}/matches/{matchId}"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/matches/{matchId}/sheet"),
                new Endpoint(HttpMethod.POST, "/api/leagues/{id}/matches/{matchId}/close"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/matches/{matchId}/audit"));
    }

    @Test
    @DisplayName("The scan covers the squad endpoints")
    void scanCoversTheSquadEndpoints() {
        assertThat(apiEndpoints()).contains(
                new Endpoint(HttpMethod.POST, "/api/leagues/{id}/players"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/players"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/players/{playerId}"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/players/{playerId}"),
                new Endpoint(HttpMethod.DELETE, "/api/leagues/{id}/players/{playerId}"),
                new Endpoint(HttpMethod.POST, "/api/leagues/{id}/players/{playerId}/photo/upload"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/players/{playerId}/photo"),
                new Endpoint(HttpMethod.DELETE, "/api/leagues/{id}/players/{playerId}/photo"));
    }

    @Test
    @DisplayName("The scan covers the lineup endpoints")
    void scanCoversTheLineupEndpoints() {
        assertThat(apiEndpoints()).contains(
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/lineup"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/lineup"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/lineups/{matchId}"));
    }

    @Test
    @DisplayName("The scan covers the scoring endpoints")
    void scanCoversTheScoringEndpoints() {
        assertThat(apiEndpoints()).contains(
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/standings"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/standings/{matchId}"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/managers/{userId}/history"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/players/{playerId}/points"));
    }

    @Test
    @DisplayName("The scan covers the rating endpoints")
    void scanCoversTheRatingEndpoints() {
        assertThat(apiEndpoints()).contains(
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/ratings"),
                new Endpoint(HttpMethod.GET, "/api/leagues/{id}/ratings/{matchId}"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/ratings/{matchId}"),
                new Endpoint(HttpMethod.PUT, "/api/leagues/{id}/ratings/{matchId}/exclusions/{userId}"));
    }

    @Test
    @DisplayName("Every API endpoint requires a session")
    void everyApiEndpointRequiresASession() throws Exception {
        for (Endpoint e : apiEndpoints()) {
            mvc.perform(MockMvcRequestBuilders.request(e.method(), e.path()).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Every mutating API endpoint requires a CSRF token")
    void everyMutatingApiEndpointRequiresCsrf() throws Exception {
        for (Endpoint e : apiEndpoints()) {
            if (MUTATING.contains(e.method())) {
                mvc.perform(MockMvcRequestBuilders.request(e.method(), e.path()).with(oidcLogin()))
                        .andExpect(status().isForbidden());
            }
        }
    }
}
