package com.buddyfantasy.shared.infrastructure;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
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

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * FE-34: the E2E tests' clock, against the real server as Playwright uses it (a session from the test login, and the
 * CSRF header on the POST). With the {@code e2e} profile the jobs run every 2 s on their own, so moving the clock
 * forward is enough to lock the lineups and close the rating windows. The clock only moves forward and is shared by
 * the whole context, so each test starts from its current time. Without the profile it does not exist
 * ({@code SessionTests}). The profile's jobs run with the real time, so the context is closed after the class:
 * cached, it would lock and close the data of the other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Import(PostgresTestConfig.class)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class E2eClockTests {

    /** Longer than a few runs of the jobs, every 2 s with the profile. */
    static final Duration JOB_TIMEOUT = Duration.ofSeconds(15);

    @LocalServerPort
    int port;

    @Autowired
    E2eClock clock;

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    /** A logged-in browser: keeps its cookies and does not follow redirects. */
    CookieManager cookies;
    HttpClient browser;

    @BeforeEach
    void logIn() throws Exception {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        browser = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        get("/actuator/health");
        assertThat(post("/api/e2e/login", "{\"email\":\"ana@x.com\",\"displayName\":\"Ana\"}").statusCode())
                .isEqualTo(204);
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return browser.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** A POST as Playwright sends it, with the CSRF cookie's value echoed in {@code X-XSRF-TOKEN}. */
    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        String csrf = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst().orElseThrow();
        return browser.send(HttpRequest.newBuilder(url(path))
                        .header("X-XSRF-TOKEN", csrf)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /** The server time of a {@code 200} from the clock endpoint. */
    private Instant now(HttpResponse<String> response) throws IOException {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return Instant.parse(json.readTree(response.body()).path("now").asText());
    }

    private HttpResponse<String> advance(String duration) throws IOException, InterruptedException {
        return post("/api/e2e/clock", "{\"advance\":\"" + duration + "\"}");
    }

    /** A league of the default parameters (7 players, locked 2 h before kickoff) with the admin's lineup saved. */
    private LineupDue lineupDue() {
        UUID admin = fixtures.user("admin");
        TestLeague league = fixtures.league(admin);
        List<UUID> squad = fixtures.squad(league, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD);
        UUID match = fixtures.schedule(league, 1, clock.instant().plus(3, ChronoUnit.HOURS));
        bus.dispatch(new SaveLineupCommand(admin, league.id(), squad));
        return new LineupDue(league, squad, match);
    }

    record LineupDue(TestLeague league, List<UUID> squad, UUID match) {
    }

    private boolean locked(UUID match) {
        return jdbc.queryForObject("select count(*) from lineup_locked_lineup where match_id = ?", Integer.class,
                match) == 1;
    }

    private boolean ratingsClosed(UUID match) {
        return jdbc.queryForObject("select closed_at is not null from rating_window where match_id = ?",
                Boolean.class, match);
    }

    @Test
    @DisplayName("The test clock tells the server time and moves it forward")
    void theTestClockTellsTheServerTimeAndMovesItForward() throws Exception {
        Instant before = now(get("/api/e2e/clock"));
        assertThat(before).isCloseTo(clock.instant(), within(5, ChronoUnit.SECONDS))
                .isAfterOrEqualTo(Instant.now().minusSeconds(5));

        Instant advanced = now(advance("PT26H"));

        assertThat(advanced).isCloseTo(before.plus(26, ChronoUnit.HOURS), within(5, ChronoUnit.SECONDS));
        assertThat(clock.instant()).isAfterOrEqualTo(advanced);
        assertThat(now(get("/api/e2e/clock"))).isAfterOrEqualTo(advanced);
    }

    @Test
    @DisplayName("The test clock only moves forward, and only with the CSRF token")
    void theTestClockOnlyMovesForward() throws Exception {
        Instant before = clock.instant();

        assertThat(advance("-PT1H").statusCode()).isEqualTo(400);
        assertThat(advance("PT0S").statusCode()).isEqualTo(400);
        assertThat(post("/api/e2e/clock", "{}").statusCode()).isEqualTo(400);
        HttpResponse<String> withoutCsrf = browser.send(HttpRequest.newBuilder(url("/api/e2e/clock"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"advance\":\"PT1H\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(withoutCsrf.statusCode()).isEqualTo(403);

        assertThat(clock.instant()).isCloseTo(before, within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Moving the test clock past the lock locks the lineups without running the job by hand")
    void movingTheTestClockPastTheLockLocksTheLineups() throws Exception {
        LineupDue due = lineupDue();
        assertThat(locked(due.match())).isFalse();

        now(advance("PT2H"));

        Awaitility.await().atMost(JOB_TIMEOUT).until(() -> locked(due.match()));
    }

    @Test
    @DisplayName("Moving the test clock past the rating window closes it without running the job by hand")
    void movingTheTestClockPastTheRatingWindowClosesIt() throws Exception {
        LineupDue due = lineupDue();
        now(advance("PT4H"));
        fixtures.closeWithSheet(due.league(), due.match(), 1, 0, played(due.squad().getFirst(), 1));
        assertThat(ratingsClosed(due.match())).isFalse();

        now(advance("PT25H"));

        Awaitility.await().atMost(JOB_TIMEOUT).until(() -> ratingsClosed(due.match()));
    }

    @Test
    @DisplayName("Starting with the e2e profile logs a warning about the test clock")
    void startingWithTheE2eProfileLogsAWarningAboutTheTestClock(CapturedOutput output) {
        assertThat(output.getAll().lines()).anyMatch(line -> line.contains("WARN")
                && line.contains("Profile 'e2e' active: /api/e2e/clock moves the server clock forward"));
    }
}
