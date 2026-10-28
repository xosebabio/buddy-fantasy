package com.buddyfantasy.identity.adapter.in.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.identity.adapter.in.web.FakeGoogle.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-01, RF-04: the session against the real server, with the whole Google login ({@link FakeGoogle} answers for
 * Google). The session lives in Spring Session JDBC and travels in the {@code SESSION} cookie, which the SPA's
 * JavaScript cannot read ({@code HttpOnly}), is not sent by cross-site requests ({@code SameSite=Lax}) and, with
 * {@code SESSION_COOKIE_SECURE=true} as in production, only travels over HTTPS.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfig.class)
class SessionTests {

    static final Account ANA = new Account("google-ana", "ana@x.com", "Ana", true);

    @DynamicPropertySource
    static void google(DynamicPropertyRegistry registry) {
        FakeGoogle.register(registry);
    }

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

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return browser.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /** A POST as the SPA sends it: the CSRF cookie's value echoed in the {@code X-XSRF-TOKEN} header. */
    private HttpResponse<String> post(String path) throws IOException, InterruptedException {
        String csrf = cookie("XSRF-TOKEN");
        assertThat(csrf).as("XSRF-TOKEN cookie").isNotNull();
        return browser.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("X-XSRF-TOKEN", csrf)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst().orElse(null);
    }

    /** The {@code Set-Cookie} headers of {@code name}, as sent. */
    private static List<String> setCookies(HttpResponse<?> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith(name + "="))
                .toList();
    }

    /** The attributes of a {@code Set-Cookie} header, lower-cased by name, without its value. */
    private static Map<String, String> attributes(String setCookie) {
        return Arrays.stream(setCookie.split(";")).skip(1)
                .map(String::trim)
                .collect(Collectors.toMap(attribute -> attribute.split("=", 2)[0].toLowerCase(),
                        attribute -> attribute.contains("=") ? attribute.split("=", 2)[1] : ""));
    }

    private static Map<String, String> query(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(parameter -> parameter[0],
                        parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8)));
    }

    /** The whole login: to Google and back to the callback with a code, which the backend exchanges. */
    private HttpResponse<String> logIn(Account account) throws IOException, InterruptedException {
        HttpResponse<String> toGoogle = get("/oauth2/authorization/google");
        assertThat(toGoogle.statusCode()).isEqualTo(302);
        Map<String, String> authorization = query(URI.create(toGoogle.headers().firstValue("Location").orElseThrow()));
        FakeGoogle.nextLogin(account, authorization.get("nonce"));

        return get("/login/oauth2/code/google?code=test-code&state="
                + URLEncoder.encode(authorization.get("state"), StandardCharsets.UTF_8));
    }

    private int sessions() {
        return jdbc.queryForObject("select count(*) from spring_session", Integer.class);
    }

    @Test
    @DisplayName("Starting the login stores the session in the database")
    void startingTheLoginStoresTheSessionInTheDatabase() throws Exception {
        HttpResponse<String> toGoogle = get("/oauth2/authorization/google");

        assertThat(toGoogle.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).startsWith("https://accounts.google.com/"));
        assertThat(sessions()).isOne();
        assertThat(setCookies(toGoogle, "SESSION")).singleElement().satisfies(setCookie ->
                assertThat(attributes(setCookie))
                        .containsKey("httponly")
                        .containsEntry("samesite", "Lax")
                        .doesNotContainKey("secure"));
    }

    /**
     * Against the real server: MockMvc's {@code csrf()} swaps the CSRF repository of the cached context for one
     * without cookies, so a MockMvc test of the cookie depends on which test ran before it.
     */
    @Test
    @DisplayName("The CSRF cookie is readable by the SPA")
    void theCsrfCookieIsReadableByTheSpa() throws Exception {
        HttpResponse<String> health = get("/actuator/health");

        assertThat(setCookies(health, "XSRF-TOKEN")).singleElement().satisfies(setCookie ->
                assertThat(attributes(setCookie)).doesNotContainKey("httponly"));
    }

    @Test
    @DisplayName("Logging in registers the user and authenticates the stored session")
    void loggingInRegistersTheUserAndAuthenticatesTheStoredSession() throws Exception {
        HttpResponse<String> callback = logIn(ANA);

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).doesNotContain("error"));
        // a new session id on login (session fixation), still in the same row
        assertThat(setCookies(callback, "SESSION")).isNotEmpty();
        assertThat(jdbc.queryForList("select principal_name from spring_session", String.class))
                .containsExactly(ANA.sub());

        HttpResponse<String> me = get("/api/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"email\":\"ana@x.com\"", "\"displayName\":\"Ana\"");
        assertThat(jdbc.queryForObject("select count(*) from identity_user where email = ?", Integer.class,
                ANA.email())).isOne();
    }

    @Test
    @DisplayName("An account without a verified email cannot log in")
    void anAccountWithoutAVerifiedEmailCannotLogIn() throws Exception {
        HttpResponse<String> callback = logIn(new Account("google-eve", "eve@x.com", "Eve", false));

        assertThat(callback.statusCode()).isEqualTo(302);
        assertThat(callback.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).contains("error"));
        assertThat(get("/api/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select count(*) from identity_user", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Logging out deletes the session and expires its cookie")
    void loggingOutDeletesTheSessionAndExpiresItsCookie() throws Exception {
        logIn(ANA);
        assertThat(get("/api/me").statusCode()).isEqualTo(200);
        String session = cookie("SESSION");

        HttpResponse<String> logout = post("/api/logout");

        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(setCookies(logout, "SESSION")).isNotEmpty().allSatisfy(setCookie ->
                assertThat(attributes(setCookie)).containsEntry("max-age", "0"));
        assertThat(sessions()).isZero();
        // the old cookie no longer opens a session, even if the browser kept it
        HttpResponse<String> replayed = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/me"))
                        .header("Cookie", "SESSION=" + session).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(replayed.statusCode()).isEqualTo(401);
    }

    @Nested
    @TestPropertySource(properties = "SESSION_COOKIE_SECURE=true")
    class InProduction {

        @LocalServerPort
        int productionPort;

        /** The outer instance is injected from the outer context: point the browser at this context's server. */
        @BeforeEach
        void useThisServer() {
            port = productionPort;
        }

        @Test
        @DisplayName("The session cookie only travels over HTTPS")
        void theSessionCookieOnlyTravelsOverHttps() throws Exception {
            HttpResponse<String> toGoogle = get("/oauth2/authorization/google");

            assertThat(setCookies(toGoogle, "SESSION")).singleElement().satisfies(setCookie ->
                    assertThat(attributes(setCookie))
                            .containsKey("secure")
                            .containsKey("httponly")
                            .containsEntry("samesite", "Lax"));
        }
    }
}
