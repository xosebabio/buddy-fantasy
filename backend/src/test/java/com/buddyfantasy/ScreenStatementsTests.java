package com.buddyfantasy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.lineup.application.usecases.lockmatchlineups.LockMatchLineupsCommand;
import com.buddyfantasy.lineup.application.usecases.savelineup.SaveLineupCommand;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.rating.application.usecases.excluderater.ExcludeRaterCommand;
import com.buddyfantasy.rating.application.usecases.savemyratings.SaveMyRatingsCommand;
import com.buddyfantasy.rating.domain.Vote;
import com.buddyfantasy.shared.application.CommandBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The main screens run the same number of SQL statements whatever the volume (RNF-09): no N+1. Each screen is
 * measured on a small league and again once it has the volume of a real season (20 members, 25 players, 30 matches,
 * the locked lineups of 20 managers and the votes of every member, two of them scored), counting every statement with
 * {@link SqlStatements}. No time assertions: they would be flaky.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class ScreenStatementsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Duration WEEK = Duration.ofDays(7);
    static final int LINEUP_SIZE = 7;

    @Autowired
    MockMvc mvc;

    @Autowired
    CommandBus bus;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

    UUID admin;
    UUID ana;
    TestLeague peña;
    final List<UUID> members = new ArrayList<>();
    final List<UUID> players = new ArrayList<>();
    final List<UUID> matches = new ArrayList<>();

    /**
     * Each screen as ana sees it (the audit and the exclusions, as the admin), the match screens on {@code matchId},
     * the history of her own points and the points of the first player, who is in every sheet.
     */
    private Map<String, MockHttpServletRequestBuilder> screens(UUID matchId) {
        UUID league = peña.id();
        Map<String, MockHttpServletRequestBuilder> screens = new LinkedHashMap<>();
        screens.put("my leagues", get("/api/me/leagues").with(as(ana)));
        screens.put("players", get("/api/leagues/{id}/players", league).with(as(ana)));
        screens.put("matches", get("/api/leagues/{id}/matches", league).with(as(ana)));
        screens.put("match", get("/api/leagues/{id}/matches/{matchId}", league, matchId).with(as(ana)));
        screens.put("match audit", get("/api/leagues/{id}/matches/{matchId}/audit", league, matchId).with(as(admin)));
        screens.put("members", get("/api/leagues/{id}/members", league).with(as(ana)));
        screens.put("my lineup", get("/api/leagues/{id}/lineup", league).with(as(ana)));
        screens.put("locked lineups", get("/api/leagues/{id}/lineups/{matchId}", league, matchId).with(as(ana)));
        screens.put("standings", get("/api/leagues/{id}/standings", league).with(as(ana)));
        screens.put("matchday standings", get("/api/leagues/{id}/standings/{matchId}", league, matchId).with(as(ana)));
        screens.put("manager history",
                get("/api/leagues/{id}/managers/{userId}/history", league, ana).with(as(ana)));
        screens.put("player points",
                get("/api/leagues/{id}/players/{playerId}/points", league, players.get(0)).with(as(ana)));
        screens.put("rating windows", get("/api/leagues/{id}/ratings", league).with(as(ana)));
        screens.put("match ratings", get("/api/leagues/{id}/ratings/{matchId}", league, matchId).with(as(ana)));
        screens.put("match ratings (admin)",
                get("/api/leagues/{id}/ratings/{matchId}", league, matchId).with(as(admin)));
        return screens;
    }

    /** The statements of each screen, which must answer 200. */
    private Map<String, List<String>> statements(UUID matchId) throws Exception {
        Map<String, List<String>> statements = new LinkedHashMap<>();
        for (Map.Entry<String, MockHttpServletRequestBuilder> screen : screens(matchId).entrySet()) {
            statements.put(screen.getKey(),
                    SqlStatements.during(() -> mvc.perform(screen.getValue()).andExpect(status().isOk())));
        }
        return statements;
    }

    private static Map<String, Integer> counts(Map<String, List<String>> statements) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        statements.forEach((screen, list) -> counts.put(screen, list.size()));
        return counts;
    }

    private void addMembers(int count) {
        for (int i = members.size(); members.size() < count; i++) {
            UUID member = fixtures.user("manager" + i);
            fixtures.join(peña, member);
            members.add(member);
        }
    }

    private void addPlayers(int count) {
        List<Position> positions = List.of(GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD);
        while (players.size() < count) {
            players.add(fixtures.player(peña, "Player " + players.size(), players.size() + 1,
                    positions.get(players.size() % positions.size())));
        }
    }

    private void addMatches(int count) {
        while (matches.size() < count) {
            int matchday = matches.size() + 1;
            matches.add(fixtures.schedule(peña, matchday, KICKOFF_1.plus(WEEK.multipliedBy(matchday - 1))));
        }
    }

    /** Every member saves a lineup of 7 of the players, each one starting at a different player. */
    private void saveLineups() {
        for (int i = 0; i < members.size(); i++) {
            int first = i;
            List<UUID> lineup = IntStream.range(0, LINEUP_SIZE)
                    .mapToObj(offset -> players.get((first + offset) % players.size()))
                    .toList();
            bus.dispatch(new SaveLineupCommand(members.get(i), peña.id(), lineup));
        }
    }

    /** A line for each of the first {@code lines} players, the first one scoring {@code goals}. */
    private PerformanceLine[] sheet(int lines, int goals) {
        return IntStream.range(0, lines)
                .mapToObj(i -> played(players.get(i), i == 0 ? goals : 0))
                .toArray(PerformanceLine[]::new);
    }

    /**
     * Locks the match's lineups, closes it with a line for each of the first {@code lines} players, lets every member
     * rate all of them, the admin exclude the first {@code excluded} managers and then closes its rating window, so it
     * is scored.
     */
    private void play(UUID matchId, Instant kickoffAt, int lines, int excluded) {
        clock.now = kickoffAt.minus(Duration.ofHours(2));
        bus.dispatch(new LockMatchLineupsCommand(peña.id(), matchId));
        clock.now = kickoffAt.plus(Duration.ofHours(2));
        fixtures.closeWithSheet(peña, matchId, 0, 0, sheet(lines, 0));
        for (int i = 0; i < members.size(); i++) {
            int value = i % 5;
            List<Vote> votes = players.subList(0, lines).stream().map(player -> new Vote(player, value)).toList();
            bus.dispatch(new SaveMyRatingsCommand(members.get(i), peña.id(), matchId, votes));
        }
        members.stream().filter(member -> !member.equals(admin) && !member.equals(ana)).limit(excluded)
                .forEach(member -> bus.dispatch(new ExcludeRaterCommand(admin, peña.id(), matchId, member)));
        fixtures.closeRatings(clock, matchId);
    }

    @Test
    @DisplayName("The main screens run the same statements whatever the data volume (no N+1)")
    void theMainScreensRunTheSameStatementsWhateverTheVolume() throws Exception {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        peña = fixtures.league(admin);
        fixtures.join(peña, ana);
        fixtures.settings(peña, settings -> {
            settings.lineupSize = LINEUP_SIZE;
            settings.lineupMaxChanges = LINEUP_SIZE;
            settings.lineupFormationEnabled = false;
            settings.lineupBudgetEnabled = true;
        });
        members.addAll(List.of(admin, ana));

        // small: 2 members, 7 players, 2 matches, the first one played with 1 line and rated by both
        addPlayers(LINEUP_SIZE);
        addMatches(2);
        saveLineups();
        play(matches.get(0), KICKOFF_1, 1, 0);
        Map<String, List<String>> small = statements(matches.get(0));

        // a season: 20 members, 25 players, 30 matches, the second one played with 14 lines, rated by every member but
        // the 3 the admin excluded and corrected twice (so scored three times), ana in 4 leagues and with several
        // versions of her lineup
        addMembers(20);
        addPlayers(25);
        addMatches(30);
        for (int i = 0; i < 3; i++) {
            fixtures.join(fixtures.league(fixtures.user("other-admin" + i), "Otra " + i), ana);
        }
        saveLineups();
        for (int i = 1; i <= 3; i++) {
            bus.dispatch(new SaveLineupCommand(ana, peña.id(), players.subList(i, i + LINEUP_SIZE)));
        }
        Instant kickoff2 = KICKOFF_1.plus(WEEK);
        play(matches.get(1), kickoff2, 14, 3);
        fixtures.sheet(peña, matches.get(1), 1, 0, sheet(14, 1));
        fixtures.sheet(peña, matches.get(1), 2, 0, sheet(14, 2));
        Listeners.await(jdbc); // the recalculation of the corrections
        Map<String, List<String>> season = statements(matches.get(1));

        // the proxy sees them: a screen without statements would make the comparison vacuous
        assertThat(counts(small)).allSatisfy((screen, count) -> assertThat(count).as(screen).isPositive());
        assertThat(counts(season))
                .as("statements per screen; small league: %s%nseason: %s", small, season)
                .isEqualTo(counts(small));
    }
}
