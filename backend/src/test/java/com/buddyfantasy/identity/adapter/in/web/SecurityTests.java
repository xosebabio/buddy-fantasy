package com.buddyfantasy.identity.adapter.in.web;

import java.util.List;

import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class SecurityTests {

    /** The health endpoint and its Kubernetes probes, the only public paths besides the Google login. */
    static final List<String> PUBLIC = List.of("/actuator/health", "/actuator/health/liveness",
            "/actuator/health/readiness");

    @Autowired
    MockMvc mvc;

    @Autowired
    PathMappedEndpoints actuatorEndpoints;

    @Test
    @DisplayName("An unauthenticated API call is answered with 401 without a redirect")
    void unauthenticatedApiCallIs401WithoutRedirect() throws Exception {
        mvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("The health endpoint is public")
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("The health probes are public")
    void healthProbesArePublic() throws Exception {
        for (String path : PUBLIC) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    /**
     * Scans the exposed actuator endpoints, so one exposed later is covered without listing it. Without a session a
     * non-browser request gets 401 (a browser is sent to the Google login instead).
     */
    @Test
    @DisplayName("Every other actuator endpoint requires a session")
    void everyOtherActuatorEndpointRequiresASession() throws Exception {
        List<String> paths = actuatorEndpoints.getAllPaths().stream().filter(path -> !PUBLIC.contains(path)).toList();

        assertThat(paths).contains("/actuator/info", "/actuator/modulith");
        for (String path : paths) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(oidcLogin())).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("The API docs require a session")
    void apiDocsRequireASession() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs").with(oidcLogin())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger UI is off by default")
    void swaggerUiIsOffByDefault() throws Exception {
        mvc.perform(get("/swagger-ui.html").with(oidcLogin())).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html").with(oidcLogin())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The OAuth entry point redirects to Google")
    void oauthEntryPointRedirectsToGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://accounts.google.com/**"));
    }

    @Test
    @DisplayName("A POST without a CSRF token is forbidden")
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/logout").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Logging out with a CSRF token returns 204")
    void logoutWithCsrfTokenReturns204() throws Exception {
        mvc.perform(post("/api/logout").with(oidcLogin()).with(csrf())).andExpect(status().isNoContent());
    }
}
