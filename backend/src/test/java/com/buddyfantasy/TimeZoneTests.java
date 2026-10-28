package com.buddyfantasy;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Instants travel unchanged through the API, the database and the lock (RNF-05): UTC in the backend, whatever the
 * zone of the JVM and of Postgres, which the tests run in Europe/Madrid ({@code argLine} in the {@code pom.xml} and
 * {@link PostgresTestConfig}). The instants are around Madrid's changes of time, where a conversion through local
 * time skips or merges an hour.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class TimeZoneTests {

    /** Madrid moves from 02:00 CET to 03:00 CEST at 01:00Z: 03:30 local time, the day of the change. */
    static final String SPRING_KICKOFF = "2026-03-29T01:30:00Z";
    /** Madrid moves back from 03:00 CEST to 02:00 CET at 01:00Z: both are 02:30 local time. */
    static final String AUTUMN_KICKOFF_CEST = "2026-10-25T00:30:00Z";
    static final String AUTUMN_KICKOFF_CET = "2026-10-25T01:30:00Z";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    LockDueLineupsJob job;

    UUID admin;
    TestLeague peña;

    @BeforeEach
    void setUp() {
        clock.now = Instant.parse("2026-03-01T10:00:00Z");
        admin = fixtures.user("admin");
        peña = fixtures.league(admin);
    }

    private ResultActions schedule(int matchday, String kickoffAt) throws Exception {
        return mvc.perform(post("/api/leagues/{id}/matches", peña.id()).with(as(admin)).with(csrf())
                .contentType(APPLICATION_JSON).content("""
                        {"matchday": %d, "opponent": "Rival", "venue": "HOME", "kickoffAt": "%s"}"""
                        .formatted(matchday, kickoffAt)));
    }

    private UUID scheduled(int matchday, String kickoffAt) throws Exception {
        String location = schedule(matchday, kickoffAt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kickoffAt").value(kickoffAt))
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    /** The stored instant as seconds since the epoch, computed by Postgres: no driver conversion in between. */
    private long storedEpoch(String sql, UUID id) {
        return jdbc.queryForObject("select extract(epoch from (" + sql + "))::bigint", Long.class, id);
    }

    @Test
    @DisplayName("The tests run outside UTC")
    void theTestsRunOutsideUtc() {
        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of(PostgresTestConfig.TIME_ZONE));
        assertThat(jdbc.queryForObject("show timezone", String.class)).isEqualTo(PostgresTestConfig.TIME_ZONE);
    }

    @Test
    @DisplayName("Kickoffs around the daylight saving changes travel unchanged")
    void kickoffsAroundTheChangesOfTimeTravelUnchanged() throws Exception {
        List<String> kickoffs = List.of(SPRING_KICKOFF, AUTUMN_KICKOFF_CEST, AUTUMN_KICKOFF_CET);
        for (int i = 0; i < kickoffs.size(); i++) {
            String kickoffAt = kickoffs.get(i);
            UUID matchId = scheduled(i + 1, kickoffAt);

            mvc.perform(get("/api/leagues/{id}/matches/{matchId}", peña.id(), matchId).with(as(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.kickoffAt").value(kickoffAt));
            assertThat(storedEpoch("select kickoff_at from match_match where id = ?", matchId))
                    .isEqualTo(Instant.parse(kickoffAt).getEpochSecond());
            assertThat(jdbc.queryForObject("select after ->> 'kickoffAt' from match_audit_entry where match_id = ?",
                    String.class, matchId)).isEqualTo(kickoffAt);
        }

        // the two autumn matches at 02:30 local time stay an hour apart
        mvc.perform(get("/api/leagues/{id}/matches", peña.id()).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].kickoffAt").value(contains(kickoffs.toArray())));
    }

    @Test
    @DisplayName("An instant with an offset is the same instant in UTC")
    void anInstantWithAnOffsetIsTheSameInstantInUtc() throws Exception {
        // 02:30 CET, the second 02:30 of the autumn change
        schedule(1, "2026-10-25T02:30:00+01:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kickoffAt").value(AUTUMN_KICKOFF_CET));
    }

    @Test
    @DisplayName("The lineup lock is the configured hours before kickoff across the daylight saving change")
    void theLineupLockIsHoursBeforeKickoffAcrossTheChangeOfTime() throws Exception {
        fixtures.settings(peña, settings -> settings.lineupLockHours = 2);
        List<UUID> squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, DEFENDER, MIDFIELDER, MIDFIELDER, FORWARD,
                FORWARD);
        UUID matchId = scheduled(1, SPRING_KICKOFF);
        String lineup = squad.stream().map(id -> "\"" + id + "\"")
                .reduce((first, second) -> first + "," + second).orElseThrow();
        // 2 h before kickoff, although Madrid's clock moves 3 h: from 00:30 CET to 03:30 CEST
        String lockAt = "2026-03-28T23:30:00Z";

        mvc.perform(put("/api/leagues/{id}/lineup", peña.id()).with(as(admin)).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"playerIds\":[" + lineup + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").value("2026-03-01T10:00:00Z"))
                .andExpect(jsonPath("$.targetMatch.lockAt").value(lockAt));
        clock.now = Instant.parse(lockAt).plusSeconds(60);
        job.lockDueLineups();

        assertThat(storedEpoch("select lock_at from lineup_match_lock where match_id = ?", matchId))
                .isEqualTo(Instant.parse(lockAt).getEpochSecond());
        mvc.perform(get("/api/leagues/{id}/lineups/{matchId}", peña.id(), matchId).with(as(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kickoffAt").value(SPRING_KICKOFF))
                .andExpect(jsonPath("$.lockAt").value(lockAt))
                .andExpect(jsonPath("$.lockedAt").value("2026-03-28T23:31:00Z"));
    }
}
