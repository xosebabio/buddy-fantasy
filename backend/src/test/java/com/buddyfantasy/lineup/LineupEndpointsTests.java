package com.buddyfantasy.lineup;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.identity.application.usecases.registerorloginuser.RegisterOrLoginUserCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Saving and reading one's working lineup (RF-24, RF-26, RF-28, RF-29) with a controllable server clock. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class LineupEndpointsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    /** 2 h before {@link #KICKOFF_1}: the league locks lineups 2 h before kickoff. */
    static final Instant LOCK_1 = Instant.parse("2026-01-11T08:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    UUID admin;
    UUID ana;
    UUID outsider;
    UUID league;
    /** 12 active players of 10 M: 2 goalkeepers, 4 defenders, 4 midfielders and 2 forwards, in this order. */
    List<UUID> squad;
    /** The first 7 of {@link #squad}: 70 M, within the 100 M budget. */
    List<UUID> lineupA;
    UUID expensive;
    UUID foreign;
    UUID otherLeague;
    UUID match1;
    UUID match2;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        jdbc.update("delete from lineup_locked_player");
        jdbc.update("delete from lineup_locked_lineup");
        jdbc.update("delete from lineup_match_lock");
        jdbc.update("delete from lineup_version");
        jdbc.update("delete from match_audit_entry");
        jdbc.update("delete from match_match");
        jdbc.update("delete from squad_player");
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_invitation");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
        jdbc.update("delete from identity_user");
        admin = register("admin");
        ana = register("ana");
        outsider = register("outsider");
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(admin, "Peña"));
        league = created.id().value();
        bus.dispatch(new JoinLeagueCommand(ana, created.invitationToken()));
        jdbc.update("""
                update league_settings
                set lineup_size = 7, lineup_lock_hours = 2, lineup_max_changes = 2, lineup_formation_enabled = false,
                    lineup_budget_enabled = true, lineup_budget = 100
                where league_id = ?""", league);
        // outsider is ADMIN of another league, never a member of "Peña"
        otherLeague = bus.dispatch(new CreateLeagueCommand(outsider, "Otra")).id().value();

        squad = new ArrayList<>();
        Stream.of(Position.GOALKEEPER, Position.GOALKEEPER, Position.DEFENDER, Position.DEFENDER,
                        Position.DEFENDER, Position.DEFENDER, Position.MIDFIELDER, Position.MIDFIELDER,
                        Position.MIDFIELDER, Position.MIDFIELDER, Position.FORWARD, Position.FORWARD)
                .forEach(position -> squad.add(player(league, admin, position, "10")));
        lineupA = List.copyOf(squad.subList(0, 7));
        expensive = player(league, admin, Position.FORWARD, "50");
        foreign = player(otherLeague, outsider, Position.FORWARD, "10");

        match1 = bus.dispatch(new ScheduleMatchCommand(admin, league, 1, "Rival", Venue.HOME, KICKOFF_1)).value();
        match2 = bus.dispatch(new ScheduleMatchCommand(admin, league, 2, "Rival", Venue.AWAY, KICKOFF_2)).value();
    }

    private UUID register(String sub) {
        return bus.dispatch(new RegisterOrLoginUserCommand(sub, sub + "@x.com", "Name " + sub, null)).value();
    }

    private UUID player(UUID leagueId, UUID leagueAdmin, Position position, String value) {
        return bus.dispatch(new RegisterPlayerCommand(leagueAdmin, leagueId, "Player", null, position,
                new BigDecimal(value))).value();
    }

    private static RequestPostProcessor as(UUID userId) {
        OidcIdToken idToken = new OidcIdToken("t", NOW, NOW.plusSeconds(3600), Map.of("sub", "any"));
        return oidcLogin().oidcUser(new CurrentUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, null, userId));
    }

    private ResultActions save(UUID userId, List<UUID> playerIds) throws Exception {
        return mvc.perform(put("/api/leagues/{id}/lineup", league).with(as(userId)).with(csrf())
                .contentType(APPLICATION_JSON).content(body(playerIds)));
    }

    private ResultActions myLineup(UUID userId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/lineup", league).with(as(userId)));
    }

    private ResultActions lockedLineups(UUID userId, UUID leagueId, UUID matchId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/lineups/{matchId}", leagueId, matchId).with(as(userId)));
    }

    /** What the lock job does once the lock time has come. */
    private void lock(UUID matchId) {
        bus.dispatch(new LockMatchLineupsCommand(league, matchId));
    }

    private static String body(List<UUID> playerIds) {
        return playerIds.stream().map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "{\"playerIds\":[", "]}"));
    }

    /** {@code lineup} with the players at {@code positions} replaced by {@code replacements}, in order. */
    private static List<UUID> replace(List<UUID> lineup, List<Integer> positions, List<UUID> replacements) {
        List<UUID> result = new ArrayList<>(lineup);
        for (int i = 0; i < positions.size(); i++) {
            result.set(positions.get(i), replacements.get(i));
        }
        return result;
    }

    private static String[] ids(List<UUID> playerIds) {
        return playerIds.stream().map(UUID::toString).toArray(String[]::new);
    }

    private int versionsOf(UUID userId) {
        return jdbc.queryForObject("select count(*) from lineup_version where user_id = ?", Integer.class, userId);
    }

    @Test
    void beforeTheFirstSaveTheLineupIsEmpty() throws Exception {
        myLineup(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").doesNotExist())
                .andExpect(jsonPath("$.players.length()").value(0))
                .andExpect(jsonPath("$.targetMatch.matchday").value(1))
                .andExpect(jsonPath("$.targetMatch.kickoffAt").value("2026-01-11T10:00:00Z"))
                .andExpect(jsonPath("$.targetMatch.lockAt").value("2026-01-11T08:00:00Z"))
                .andExpect(jsonPath("$.maxChanges").value(2))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist())
                .andExpect(jsonPath("$.totalValue").value(0.0))
                .andExpect(jsonPath("$.budget").value(100.0))
                .andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    void managerAndAdminEachSaveTheirOwnLineup() throws Exception {
        save(ana, lineupA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").value("2026-01-10T10:00:00Z"))
                .andExpect(jsonPath("$.players[*].id", containsInAnyOrder(ids(lineupA))))
                .andExpect(jsonPath("$.players[0].position").value("GOALKEEPER"))
                .andExpect(jsonPath("$.players[0].active").value(true))
                .andExpect(jsonPath("$.players[0].value").value(10.0))
                .andExpect(jsonPath("$.targetMatch.matchday").value(1))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist())
                .andExpect(jsonPath("$.totalValue").value(70.0))
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // nobody sees another manager's working lineup
        myLineup(admin).andExpect(status().isOk()).andExpect(jsonPath("$.players.length()").value(0));

        List<UUID> adminLineup = squad.subList(5, 12);
        save(admin, adminLineup).andExpect(status().isOk());

        myLineup(ana).andExpect(jsonPath("$.players[*].id", containsInAnyOrder(ids(lineupA))));
        myLineup(admin).andExpect(jsonPath("$.players[*].id", containsInAnyOrder(ids(adminLineup))));
        assertThat(versionsOf(ana)).isEqualTo(1);
        assertThat(versionsOf(admin)).isEqualTo(1);
    }

    @Test
    void nonMembersCannotSaveNorSeeALineup() throws Exception {
        save(outsider, lineupA).andExpect(status().isForbidden());
        mvc.perform(get("/api/leagues/{id}/lineup", league).with(as(outsider))).andExpect(status().isForbidden());

        assertThat(versionsOf(outsider)).isZero();
    }

    @Test
    void invalidLineupsAre400WithACode() throws Exception {
        UUID retired = squad.get(11);
        bus.dispatch(new RetirePlayerCommand(admin, league, retired));
        Map<List<UUID>, String> invalid = Map.of(
                lineupA.subList(0, 6), "SIZE",
                squad.subList(0, 8), "SIZE",
                replace(lineupA, List.of(6), List.of(lineupA.getFirst())), "DUPLICATE_PLAYER",
                replace(lineupA, List.of(6), List.of(foreign)), "UNKNOWN_PLAYER",
                replace(lineupA, List.of(6), List.of(UUID.randomUUID())), "UNKNOWN_PLAYER",
                replace(lineupA, List.of(6), List.of(retired)), "INACTIVE_PLAYER",
                // 60 M + 50 M
                replace(lineupA, List.of(6), List.of(expensive)), "OVER_BUDGET");
        for (Map.Entry<List<UUID>, String> entry : invalid.entrySet()) {
            save(ana, entry.getKey())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(entry.getValue()));
        }
        for (String request : List.of("{}", "{\"playerIds\":null}", "{\"playerIds\":[null]}",
                "{\"playerIds\":[\"not-a-uuid\"]}")) {
            mvc.perform(put("/api/leagues/{id}/lineup", league).with(as(ana)).with(csrf())
                    .contentType(APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
        }

        assertThat(versionsOf(ana)).isZero();
    }

    @Test
    void theFormationMinimumsApplyWhenEnabled() throws Exception {
        jdbc.update("update league_settings set lineup_formation_enabled = true where league_id = ?", league);

        // defenders and midfielders only
        save(ana, squad.subList(2, 9))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORMATION"));
        save(ana, List.of(squad.get(0), squad.get(2), squad.get(3), squad.get(6), squad.get(7), squad.get(10),
                squad.get(11))).andExpect(status().isOk());
    }

    @Test
    void savingTheCurrentLineupAgainCreatesNoVersion() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = NOW.plus(Duration.ofHours(1));

        save(ana, lineupA.reversed())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").value("2026-01-10T10:00:00Z"));

        assertThat(versionsOf(ana)).isEqualTo(1);
    }

    @Test
    void changesAreUnlimitedUntilALineupIsLocked() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        // 5 new players
        save(ana, squad.subList(5, 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").doesNotExist());
    }

    @Test
    void afterTheLockTheLineupCarriesOverAndChangesCountAgainstIt() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1;

        // untouched, it carries over to matchday 2 (RF-28)
        myLineup(ana)
                .andExpect(jsonPath("$.players[*].id", containsInAnyOrder(ids(lineupA))))
                .andExpect(jsonPath("$.targetMatch.matchday").value(2))
                .andExpect(jsonPath("$.targetMatch.lockAt").value("2026-01-18T08:00:00Z"))
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").value(2));

        save(ana, replace(lineupA, List.of(5, 6), List.of(squad.get(7), squad.get(8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").value("2026-01-11T08:00:00Z"))
                .andExpect(jsonPath("$.changesUsed").value(2))
                .andExpect(jsonPath("$.changesRemaining").value(0));
        save(ana, replace(lineupA, List.of(4, 5, 6), List.of(squad.get(7), squad.get(8), squad.get(9))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAX_CHANGES_EXCEEDED"));
        // counted on the final difference: taking squad[8] out again gives the change back
        save(ana, replace(lineupA, List.of(5, 6), List.of(squad.get(7), squad.get(9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(2));
        save(ana, lineupA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.changesRemaining").value(2));

        assertThat(versionsOf(ana)).isEqualTo(4);
    }

    @Test
    void aSaveJustBeforeTheLockIsTheLockedLineup() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        List<UUID> lastMinute = replace(lineupA, List.of(5, 6), List.of(squad.get(7), squad.get(8)));
        clock.now = LOCK_1.minusSeconds(1);
        save(ana, lastMinute)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch.matchday").value(1));

        clock.now = LOCK_1;
        myLineup(ana)
                .andExpect(jsonPath("$.players[*].id", containsInAnyOrder(ids(lastMinute))))
                .andExpect(jsonPath("$.changesUsed").value(0));
        // back to lineup A costs the 2 changes
        save(ana, lineupA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(2));
    }

    @Test
    void replacingARetiredPlayerIsFree() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        UUID retired = lineupA.get(6);
        bus.dispatch(new RetirePlayerCommand(admin, league, retired));
        clock.now = LOCK_1;

        myLineup(ana)
                .andExpect(jsonPath("$.warnings", containsInAnyOrder("RETIRED_PLAYERS")))
                .andExpect(jsonPath("$.players[?(@.id == '" + retired + "')].active").value(false));
        save(ana, replace(lineupA, List.of(6), List.of(squad.get(7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(0))
                .andExpect(jsonPath("$.warnings.length()").value(0));
        // the replacement plus the 2 changes of the matchday
        save(ana, replace(lineupA, List.of(4, 5, 6), List.of(squad.get(7), squad.get(8), squad.get(9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changesUsed").value(2))
                .andExpect(jsonPath("$.changesRemaining").value(0));
    }

    @Test
    void aLineupThatNoLongerFitsTheBudgetIsWarned() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        // 60 M + 40 M: exactly the budget still fits
        jdbc.update("update squad_player set initial_value = 40 where id = ?", lineupA.getFirst());
        myLineup(ana)
                .andExpect(jsonPath("$.totalValue").value(100.0))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        jdbc.update("update squad_player set initial_value = 41 where id = ?", lineupA.getFirst());
        myLineup(ana)
                .andExpect(jsonPath("$.totalValue").value(101.0))
                .andExpect(jsonPath("$.warnings", containsInAnyOrder("OVER_BUDGET")));
        // saving it again is rejected, although it stays in force
        save(ana, lineupA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OVER_BUDGET"));
    }

    @Test
    void withoutMatchesAheadTheLineupCanStillBeSaved() throws Exception {
        clock.now = KICKOFF_2.plus(Duration.ofDays(1));

        save(ana, lineupA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetMatch").doesNotExist())
                .andExpect(jsonPath("$.changesRemaining").doesNotExist());
    }

    @Test
    void onceLockedEveryMemberSeesEveryLineupWithItsValuesAtTheLock() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        List<UUID> adminLineup = squad.subList(5, 12);
        save(admin, adminLineup).andExpect(status().isOk());
        clock.now = LOCK_1.plus(Duration.ofMinutes(1));
        lock(match1);
        // values change after the lock: the locked lineups keep theirs
        jdbc.update("update squad_player set initial_value = 40 where id = ?", lineupA.getFirst());

        for (UUID member : List.of(ana, admin)) {
            lockedLineups(member, league, match1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchId").value(match1.toString()))
                    .andExpect(jsonPath("$.matchday").value(1))
                    .andExpect(jsonPath("$.kickoffAt").value("2026-01-11T10:00:00Z"))
                    .andExpect(jsonPath("$.lockAt").value("2026-01-11T08:00:00Z"))
                    .andExpect(jsonPath("$.lockedAt").value("2026-01-11T08:01:00Z"))
                    .andExpect(jsonPath("$.lineups[*].userId", containsInAnyOrder(ana.toString(), admin.toString())))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].players[*].id",
                            containsInAnyOrder(ids(lineupA))))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + admin + "')].players[*].id",
                            containsInAnyOrder(ids(adminLineup))))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].players[?(@.id == '"
                            + lineupA.getFirst() + "')].value").value(10.0))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].totalValue").value(70.0))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].budget").value(100.0))
                    .andExpect(jsonPath("$.lineups[?(@.userId == '" + ana + "')].overBudget").value(false))
                    .andExpect(jsonPath("$.lineups[*].versionId").doesNotExist());
        }
        // the working lineup, by contrast, reflects the current value
        myLineup(ana).andExpect(jsonPath("$.totalValue").value(100.0));
    }

    @Test
    void membersWithoutALineupBeforeTheLockAreNotInIt() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1;
        // saved after the lock: counts for matchday 2
        save(admin, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1.plus(Duration.ofMinutes(1));
        lock(match1);

        lockedLineups(admin, league, match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineups[*].userId", containsInAnyOrder(ana.toString())));
    }

    @Test
    void aLineupOverBudgetAtTheLockIsShownAsSuch() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        jdbc.update("update squad_player set initial_value = 41 where id = ?", lineupA.getFirst());
        clock.now = LOCK_1.plus(Duration.ofMinutes(1));
        lock(match1);

        lockedLineups(admin, league, match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineups[0].totalValue").value(101.0))
                .andExpect(jsonPath("$.lineups[0].overBudget").value(true));
    }

    @Test
    void lineupsAreNotVisibleUntilTheMatchIsLocked() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());

        lockedLineups(admin, league, match1).andExpect(status().isConflict());
        // the lock time has come but the job has not locked it yet
        clock.now = LOCK_1;
        lockedLineups(admin, league, match1).andExpect(status().isConflict());
        lockedLineups(admin, league, match2).andExpect(status().isConflict());
    }

    @Test
    void lockedLineupsAreOnlyVisibleWithinTheirLeague() throws Exception {
        save(ana, lineupA).andExpect(status().isOk());
        clock.now = LOCK_1.plus(Duration.ofMinutes(1));
        lock(match1);
        UUID otherMatch = bus.dispatch(new ScheduleMatchCommand(outsider, otherLeague, 1, "Rival", Venue.HOME,
                KICKOFF_2)).value();

        lockedLineups(outsider, league, match1).andExpect(status().isForbidden());
        // the outsider's own league does not reach the match of "Peña", nor "Peña" the match of the other league
        lockedLineups(outsider, otherLeague, match1).andExpect(status().isNotFound());
        lockedLineups(ana, league, otherMatch).andExpect(status().isNotFound());
        lockedLineups(ana, league, UUID.randomUUID()).andExpect(status().isNotFound());
    }
}
