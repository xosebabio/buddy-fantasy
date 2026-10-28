package com.buddyfantasy.lineup;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.buddyfantasy.MutableClock;
import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.RecordedEvents;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.lineup.adapter.in.scheduler.LockDueLineupsJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lineups while the season goes on (RF-07 × RF-24 to RF-29): the admin changes the league's parameters between
 * matches, and members leave and come back (decision D2 of plan 07: they start from scratch). The lock job is run by
 * hand with a controllable server clock.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class LineupSeasonChangesTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    /** 2 h before {@link #KICKOFF_1}: the league starts locking lineups 2 h before kickoff. */
    static final Instant LOCK_1 = Instant.parse("2026-01-11T08:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    static final Instant LOCK_2 = Instant.parse("2026-01-18T08:00:00Z");
    /** The job only locks matches whose lock time is at least this old ({@code lineup.lock-job.margin}). */
    static final Duration MARGIN = Duration.ofMinutes(1);

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

    @Autowired
    RecordedEvents events;

    UUID admin;
    UUID ana;
    TestLeague peña;
    UUID league;
    /** 12 active players of 10 M: 2 goalkeepers, 4 defenders, 4 midfielders and 2 forwards, in this order. */
    List<UUID> squad;
    /** The first 7 of {@link #squad}, without forwards: 70 M, within the 100 M budget. */
    List<UUID> lineupA;
    /** {@link #lineupA} with its last two players replaced: 2 changes. */
    List<UUID> lineupB;
    UUID match1;
    UUID match2;

    /** How a member stops being one. */
    enum Exit { REMOVED, LEFT }

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        peña = fixtures.league(admin);
        league = peña.id();
        fixtures.join(peña, ana);
        fixtures.settings(peña, settings -> {
            settings.lineupSize = 7;
            settings.lineupLockHours = 2;
            settings.lineupMaxChanges = 2;
            settings.lineupFormationEnabled = false;
            settings.lineupBudgetEnabled = true;
            settings.lineupBudget = new BigDecimal("100");
        });

        squad = fixtures.squad(peña, GOALKEEPER, GOALKEEPER, DEFENDER, DEFENDER, DEFENDER, DEFENDER, MIDFIELDER,
                MIDFIELDER, MIDFIELDER, MIDFIELDER, FORWARD, FORWARD);
        lineupA = List.copyOf(squad.subList(0, 7));
        lineupB = replace(lineupA, List.of(5, 6), List.of(squad.get(7), squad.get(8)));

        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
    }

    private ResultActions save(UUID userId, List<UUID> playerIds) throws Exception {
        String body = playerIds.stream().map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "{\"playerIds\":[", "]}"));
        return mvc.perform(put("/api/leagues/{id}/lineup", league).with(as(userId)).with(csrf())
                .contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions myLineup(UUID userId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/lineup", league).with(as(userId)));
    }

    /** Ana stops being a member, an hour later joins again through the link, and the clock is then. */
    private void leaveAndComeBack(Exit exit) throws Exception {
        MockHttpServletRequestBuilder request = switch (exit) {
            case REMOVED -> delete("/api/leagues/{id}/members/{memberId}", league, ana).with(as(admin));
            case LEFT -> delete("/api/leagues/{id}/members/me", league).with(as(ana));
        };
        mvc.perform(request.with(csrf())).andExpect(status().isNoContent());
        clock.now = clock.now.plus(Duration.ofHours(1));
        fixtures.join(peña, ana);
    }

    private List<UUID> lockedMatches() {
        return jdbc.queryForList("select match_id from lineup_match_lock", UUID.class);
    }

    private Instant lockAt(UUID matchId) {
        return jdbc.queryForObject("select lock_at from lineup_match_lock where match_id = ?", Timestamp.class,
                matchId).toInstant();
    }

    private List<UUID> lockedUsers(UUID matchId) {
        return jdbc.queryForList("select user_id from lineup_locked_lineup where match_id = ?", UUID.class, matchId);
    }

    private List<UUID> lockedPlayers(UUID matchId, UUID userId) {
        return jdbc.queryForList("select player_id from lineup_locked_player where match_id = ? and user_id = ?",
                UUID.class, matchId, userId);
    }

    private int versionsOf(UUID userId) {
        return jdbc.queryForObject("select count(*) from lineup_version where user_id = ?", Integer.class, userId);
    }

    /** {@code lineup} with the players at {@code positions} replaced by {@code replacements}, in order. */
    private static List<UUID> replace(List<UUID> lineup, List<Integer> positions, List<UUID> replacements) {
        List<UUID> result = new ArrayList<>(lineup);
        for (int i = 0; i < positions.size(); i++) {
            result.set(positions.get(i), replacements.get(i));
        }
        return result;
    }

    private static List<UUID> plus(List<UUID> lineup, UUID player) {
        return Stream.concat(lineup.stream(), Stream.of(player)).toList();
    }

    private static UUID[] ids(List<UUID> playerIds) {
        return playerIds.toArray(UUID[]::new);
    }

    private static String[] strings(List<UUID> playerIds) {
        return playerIds.stream().map(UUID::toString).toArray(String[]::new);
    }

    @Test
    @DisplayName("Raising the lock hours brings forward the lock of a match not locked yet")
    void raisingTheLockHoursBringsForwardTheLockOfAMatchNotLockedYet() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        fixtures.settings(peña, settings -> settings.lineupLockHours = 4);

        myLineup(ana).andExpect(jsonPath("$.targetMatch.lockAt").value("2026-01-11T06:00:00Z"));
        // at the new lock time a save already counts for matchday 2, against the lineup locked in matchday 1
        clock.now = Instant.parse("2026-01-11T06:00:00Z");
        save(ana, lineupB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.changesUsed").value(2));
        clock.now = clock.now.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockAt(match1)).isEqualTo(Instant.parse("2026-01-11T06:00:00Z"));
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    @DisplayName("Lowering the lock hours delays the lock of a match not locked yet")
    void loweringTheLockHoursDelaysTheLockOfAMatchNotLockedYet() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        fixtures.settings(peña, settings -> settings.lineupLockHours = 1);

        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();
        assertThat(lockedMatches()).isEmpty();
        // past the old lock time, a save still counts for matchday 1, and nothing is locked yet to limit it
        save(ana, lineupB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch.matchday").value(1))
                .andExpect(jsonPath("$.targetMatch.lockAt").value("2026-01-11T09:00:00Z"))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist());

        clock.now = Instant.parse("2026-01-11T09:00:00Z").plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedMatches()).containsExactly(match1);
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupB));
    }

    @Test
    @DisplayName("Lowering the lock hours of a locked match before kickoff does not reopen it")
    void loweringTheLockHoursOfALockedMatchBeforeKickoffDoesNotReopenIt() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        // with 1 h the lock would be at 09:00, still ahead
        clock.now = LOCK_1.plus(Duration.ofMinutes(30));
        fixtures.settings(peña, settings -> settings.lineupLockHours = 1);

        myLineup(ana)
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.targetMatch.lockAt").value("2026-01-18T09:00:00Z"))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").value(2));
        save(ana, replace(lineupA, List.of(4, 5, 6), List.of(squad.get(7), squad.get(8), squad.get(9))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_CHANGES_EXCEEDED"));
        save(ana, lineupB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.changesUsed").value(2));

        assertThat(lockAt(match1)).isEqualTo(LOCK_1);
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    @DisplayName("Raising the lock hours after a lock keeps the locked lineup as the baseline")
    void raisingTheLockHoursAfterALockKeepsTheLockedLineupAsTheBaseline() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        // the last-minute lineup is the one locked
        clock.now = LOCK_1.minus(Duration.ofHours(1));
        save(ana, lineupB).andExpect(status().isOk());
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        // with 4 h the lock would have been at 06:00, before lineup B was saved
        fixtures.settings(peña, settings -> settings.lineupLockHours = 4);

        myLineup(ana)
                .andExpect(jsonPath("$.players[*].id", containsInAnyOrder(strings(lineupB))))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").value(2));
        // back to lineup A costs the 2 changes
        save(ana, lineupA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(2));
    }

    @Test
    @DisplayName("Raising the lineup size keeps the carried lineup and filling the gap is free")
    void raisingTheLineupSizeKeepsTheCarriedLineupAndFillingTheGapIsFree() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        fixtures.settings(peña, settings -> settings.lineupSize = 8);

        myLineup(ana)
                .andExpect(jsonPath("$.players.length()").value(7))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.warnings", containsInAnyOrder("SIZE_MISMATCH")));
        save(ana, plus(lineupA, squad.get(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").value(2))
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // on top of the gap, the 2 changes of the matchday
        save(ana, plus(lineupB, squad.get(9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(2));
        save(ana, plus(replace(lineupA, List.of(4, 5, 6), List.of(squad.get(7), squad.get(8), squad.get(9))),
                squad.get(10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_CHANGES_EXCEEDED"));
    }

    @Test
    @DisplayName("A carried lineup of the old size is locked as it is")
    void aCarriedLineupOfTheOldSizeIsLockedAsItIs() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        fixtures.settings(peña, settings -> settings.lineupSize = 6);

        myLineup(ana).andExpect(jsonPath("$.warnings", containsInAnyOrder("SIZE_MISMATCH")));
        // saving it again is rejected, although it stays in force
        save(ana, lineupA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SIZE"));
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    @DisplayName("A carried lineup outside a formation enabled later is locked as it is")
    void aCarriedLineupOutsideAFormationEnabledLaterIsLockedAsItIs() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        fixtures.settings(peña, settings -> {
            settings.lineupFormationEnabled = true;
            settings.lineupMinForwards = 1;
        });

        // the formation has no warning: only the size can be told apart without the rules of the save
        myLineup(ana).andExpect(jsonPath("$.warnings.length()").value(0));
        save(ana, lineupA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORMATION"));
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @Test
    @DisplayName("Disabling the budget before the lock keeps a lineup from being over budget")
    void disablingTheBudgetBeforeTheLockKeepsALineupFromBeingOverBudget() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        // 60 M + 41 M
        jdbc.update("update squad_player set initial_value = 41 where id = ?", lineupA.getFirst());
        myLineup(ana).andExpect(jsonPath("$.warnings", containsInAnyOrder("OVER_BUDGET")));

        fixtures.settings(peña, settings -> settings.lineupBudgetEnabled = false);

        myLineup(ana)
                .andExpect(jsonPath("$.totalValue").value(101.0))
                .andExpect(jsonPath("$.budget").doesNotExist())
                .andExpect(jsonPath("$.warnings.length()").value(0));
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(jdbc.queryForMap(
                "select total_value, budget, over_budget from lineup_locked_lineup where match_id = ? and user_id = ?",
                match1, ana))
                .containsEntry("total_value", new BigDecimal("101.00"))
                .containsEntry("budget", null)
                .containsEntry("over_budget", false);
        assertThat(events.stream(LineupsLocked.class)).singleElement()
                .satisfies(event -> assertThat(event.lineups()).singleElement()
                        .satisfies(lineup -> assertThat(lineup.overBudget()).isFalse()));
    }

    @ParameterizedTest
    @DisplayName("A member who comes back has no lineup and their first new one has no change limit")
    @EnumSource(Exit.class)
    void aMemberWhoComesBackHasNoLineupAndTheFirstNewOneHasNoChangeLimit(Exit exit) throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        leaveAndComeBack(exit);

        myLineup(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").doesNotExist())
                .andExpect(jsonPath("$.players.length()").value(0))
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist())
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // 5 new players against the lineup locked before leaving
        List<UUID> fresh = squad.subList(5, 12);
        save(ana, fresh)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist());

        // once the new one is locked, the limit applies again
        clock.now = LOCK_2.plus(MARGIN);
        job.lockDueLineups();
        assertThat(lockedPlayers(match2, ana)).containsExactlyInAnyOrder(ids(fresh));
        myLineup(ana).andExpect(jsonPath("$.changesRemaining").value(2));
        // the lineup locked before leaving is still the history of matchday 1
        assertThat(lockedPlayers(match1, ana)).containsExactlyInAnyOrder(ids(lineupA));
    }

    @ParameterizedTest
    @DisplayName("A member who comes back without saving is left out of the next lock")
    @EnumSource(Exit.class)
    void aMemberWhoComesBackWithoutSavingIsLeftOutOfTheNextLock(Exit exit) throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        save(admin, lineupB).andExpect(status().isOk());

        leaveAndComeBack(exit);
        clock.now = LOCK_1.plus(MARGIN);
        job.lockDueLineups();

        assertThat(lockedUsers(match1)).containsExactly(admin);
        // the old version is ignored, not deleted
        assertThat(versionsOf(ana)).isEqualTo(1);
        myLineup(ana).andExpect(jsonPath("$.players.length()").value(0));
    }
}
