package com.buddyfantasy.league;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks every {@code /api/**} mapping registered in the app, so endpoints added later are covered automatically:
 * no session → 401, mutation with a session but no CSRF token → 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class ApiSecurityTests {

    private static final Set<HttpMethod> MUTATING = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH);

    private record Endpoint(HttpMethod method, String pattern) {

        /** The pattern with every path variable replaced by a random id. */
        String path() {
            return pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    private List<Endpoint> apiEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                for (var method : info.getMethodsCondition().getMethods()) {
                    endpoints.add(new Endpoint(HttpMethod.valueOf(method.name()), pattern));
                }
            }
        }
        return endpoints;
    }

    @Test
    void scanFindsAllApiEndpoints() {
        assertThat(apiEndpoints()).hasSizeGreaterThanOrEqualTo(21);
    }

    @Test
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
    void everyApiEndpointRequiresASession() throws Exception {
        for (Endpoint e : apiEndpoints()) {
            mvc.perform(MockMvcRequestBuilders.request(e.method(), e.path()).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void everyMutatingApiEndpointRequiresCsrf() throws Exception {
        for (Endpoint e : apiEndpoints()) {
            if (MUTATING.contains(e.method())) {
                mvc.perform(MockMvcRequestBuilders.request(e.method(), e.path()).with(oidcLogin()))
                        .andExpect(status().isForbidden());
            }
        }
    }
}
