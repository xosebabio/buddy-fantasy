package com.buddyfantasy.identity.adapter.in.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.buddyfantasy.PostgresTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FE-34: the E2E tests' login, against the real server as Playwright uses it: a CSRF cookie from any GET, then the
 * POST with its header. Without the profile it does not exist ({@code SessionTests}). The output is captured from
 * the class on, so it includes the start of this context. The profile's jobs run on their own every 2 s with the real
 * time, so the context is closed after the class: cached, it would lock and close the data of the other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Import(PostgresTestConfig.class)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class E2eLoginTests {

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    /** A browser: keeps its cookies and does not follow redirects. */
    CookieManager cookies;
    HttpClient browser;

    @BeforeEach
    void openBrowser() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        browser = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return browser.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** The login as the E2E fixture sends it, with the CSRF cookie's value echoed in {@code X-XSRF-TOKEN}. */
    private HttpResponse<String> logIn(String json) throws IOException, InterruptedException {
        get("/actuator/health");
        String csrf = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst().orElseThrow();
        return browser.send(HttpRequest.newBuilder(url("/api/e2e/login"))
                        .header("X-XSRF-TOKEN", csrf)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private int users() {
        return jdbc.queryForObject("select count(*) from identity_user", Integer.class);
    }

    @Test
    @DisplayName("The test login registers the user and opens a session like Google's")
    void theTestLoginRegistersTheUserAndOpensASessionLikeGoogles() throws Exception {
        HttpResponse<String> login = logIn("{\"email\":\"ana@x.com\",\"displayName\":\"Ana\"}");

        assertThat(login.statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForList("select principal_name from spring_session", String.class))
                .containsExactly("e2e:ana@x.com");
        assertThat(jdbc.queryForObject("select google_sub from identity_user where email = ?", String.class,
                "ana@x.com")).isEqualTo("e2e:ana@x.com");

        HttpResponse<String> me = get("/api/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"email\":\"ana@x.com\"", "\"displayName\":\"Ana\"",
                "\"avatarUrl\":null");
    }

    @Test
    @DisplayName("Logging in again with the test login reuses the user")
    void loggingInAgainWithTheTestLoginReusesTheUser() throws Exception {
        logIn("{\"email\":\"ana@x.com\",\"displayName\":\"Ana\"}");
        String first = get("/api/me").body();

        openBrowser();
        assertThat(logIn("{\"email\":\"ana@x.com\",\"displayName\":\"Ana\"}").statusCode()).isEqualTo(204);

        assertThat(get("/api/me").body()).isEqualTo(first);
        assertThat(users()).isOne();
    }

    @Test
    @DisplayName("The test login without CSRF token is rejected")
    void theTestLoginWithoutCsrfTokenIsRejected() throws Exception {
        HttpResponse<String> login = browser.send(HttpRequest.newBuilder(url("/api/e2e/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"ana@x.com\",\"displayName\":\"Ana\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // the CSRF filter's 403 goes to the error page, which an anonymous request cannot see: 401
        assertThat(login.statusCode()).isEqualTo(401);
        assertThat(users()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from spring_session", Integer.class)).isZero();
    }

    @Test
    @DisplayName("The test login without an email is answered with 400")
    void theTestLoginWithoutAnEmailIs400() throws Exception {
        HttpResponse<String> login = logIn("{\"email\":\" \",\"displayName\":\"Ana\"}");

        assertThat(login.statusCode()).isEqualTo(400);
        assertThat(users()).isZero();
        assertThat(get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Starting with the e2e profile logs a warning")
    void startingWithTheE2eProfileLogsAWarning(CapturedOutput output) {
        assertThat(output.getAll().lines()).anyMatch(line -> line.contains("WARN")
                && line.contains("Profile 'e2e' active: /api/e2e/login logs anyone in without Google"));
    }
}
