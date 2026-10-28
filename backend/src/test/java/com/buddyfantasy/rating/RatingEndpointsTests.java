package com.buddyfantasy.rating;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.Listeners;
import com.buddyfantasy.MutableClock;
import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.TestFixtures;
import com.buddyfantasy.TestFixtures.TestLeague;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.rating.adapter.in.scheduler.CloseDueRatingWindowsJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.buddyfantasy.TestFixtures.played;
import static com.buddyfantasy.TestUsers.as;
import static com.buddyfantasy.datadefinition.Position.DEFENDER;
import static com.buddyfantasy.datadefinition.Position.FORWARD;
import static com.buddyfantasy.datadefinition.Position.GOALKEEPER;
import static com.buddyfantasy.datadefinition.Position.MIDFIELDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rating the players of a closed match by HTTP (RF-17 to RF-21) with a controllable server clock: each member sees
 * only their own votes, during the window and after it, and the averages only once the job closes it. The job is run
 * by hand and the listeners are awaited.
 */
@SpringBootTest
@AutoConfigureMockMvc // same context as LineupEndpointsTests
@Import({PostgresTestConfig.class, MutableClock.Config.class})
class RatingEndpointsTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");
    static final Instant KICKOFF_1 = Instant.parse("2026-01-11T10:00:00Z");
    static final Instant KICKOFF_2 = Instant.parse("2026-01-18T10:00:00Z");
    /** After the kickoff, when the admin closes the match. */
    static final Duration CLOSE_AFTER_KICKOFF = Duration.ofHours(2);
    /** The default rating window. */
    static final Duration WINDOW = Duration.ofHours(24);
    /** The fields of a player in the ratings of a match: none of them is another member's vote. */
    static final List<String> PLAYER_FIELDS = List.of("playerId", "position", "myRating", "votes", "average",
            "ratingPoints");

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    CloseDueRatingWindowsJob closeJob;

    @Autowired
    ObjectMapper json;

    UUID admin;
    UUID ana;
    UUID bea;
    UUID outsider;
    TestLeague peña;
    UUID league;
    /** Goalkeeper, defender, midfielder and forward; the first three play match 1. */
    List<UUID> squad;
    UUID keeper;
    UUID defender;
    UUID midfielder;
    UUID forward;
    UUID match1;
    UUID match2;
    TestLeague other;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        admin = fixtures.user("admin");
        ana = fixtures.user("ana");
        bea = fixtures.user("bea");
        outsider = fixtures.user("outsider");
        peña = fixtures.league(admin);
        league = peña.id();
        fixtures.join(peña, ana, bea);
        // outsider is ADMIN of another league, never a member of "Peña"
        other = fixtures.league(outsider, "Otra");
        squad = fixtures.squad(peña, GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD);
        keeper = squad.get(0);
        defender = squad.get(1);
        midfielder = squad.get(2);
        forward = squad.get(3);
        match1 = fixtures.schedule(peña, 1, KICKOFF_1);
        match2 = fixtures.schedule(peña, 2, KICKOFF_2);
    }

    /** Closes the match after kickoff with {@code playerIds}, and returns when it was closed. */
    private Instant close(TestLeague league, UUID matchId, Instant kickoffAt, List<UUID> playerIds) {
        clock.now = kickoffAt.plus(CLOSE_AFTER_KICKOFF);
        fixtures.closeWithSheet(league, matchId, 1, 0,
                playerIds.stream().map(playerId -> played(playerId, 0)).toArray(PerformanceLine[]::new));
        return clock.now;
    }

    /** Closes match 1 with the goalkeeper, the defender and the midfielder, and returns when its window closes. */
    private Instant closeMatch1() {
        return close(peña, match1, KICKOFF_1, List.of(keeper, defender, midfielder)).plus(WINDOW);
    }

    private void runJobAt(Instant at) {
        clock.now = at;
        closeJob.closeDueRatingWindows();
        Listeners.await(jdbc);
    }

    private ResultActions save(UUID userId, UUID matchId, Map<UUID, Integer> ratings) throws Exception {
        return save(userId, league, matchId, body(ratings));
    }

    private ResultActions save(UUID userId, UUID leagueId, UUID matchId, String body) throws Exception {
        return mvc.perform(put("/api/leagues/{id}/ratings/{matchId}", leagueId, matchId).with(as(userId)).with(csrf())
                .contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions ratings(UUID userId, UUID leagueId, UUID matchId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/ratings/{matchId}", leagueId, matchId).with(as(userId)));
    }

    private ResultActions windows(UUID userId) throws Exception {
        return mvc.perform(get("/api/leagues/{id}/ratings", league).with(as(userId)));
    }

    private static String body(Map<UUID, Integer> ratings) {
        return ratings.entrySet().stream()
                .map(rating -> "{\"playerId\":\"%s\",\"value\":%d}".formatted(rating.getKey(), rating.getValue()))
                .collect(Collectors.joining(",", "{\"ratings\":[", "]}"));
    }

    /** The member's vote of each player of the response, in its order (goalkeepers first); null if not rated. */
    private List<Integer> myRatings(ResultActions response) throws Exception {
        List<Integer> values = new ArrayList<>();
        players(response).forEach(player -> values.add(player.get("myRating").isNull() ? null
                : player.get("myRating").asInt()));
        return values;
    }

    private JsonNode players(ResultActions response) throws Exception {
        return json.readTree(response.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("players");
    }

    private Map<UUID, Integer> storedVotes(UUID userId) {
        return jdbc.query("select player_id, value from rating_vote where user_id = ?",
                        (rs, rowNum) -> entry(rs.getObject(1, UUID.class), rs.getInt(2)), userId).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private int votes() {
        return jdbc.queryForObject("select count(*) from rating_vote", Integer.class);
    }

    @Test
    @DisplayName("A member sees the players of the sheet, goalkeepers first, until when to rate them and the format")
    void aMemberSeesThePlayersOfTheSheet() throws Exception {
        Instant closesAt = closeMatch1();

        ratings(ana, league, match1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(match1.toString()))
                .andExpect(jsonPath("$.matchday").value(1))
                .andExpect(jsonPath("$.kickoffAt").value(KICKOFF_1.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.closesAt").value(closesAt.toString()))
                .andExpect(jsonPath("$.format").value("SPADES"))
                .andExpect(jsonPath("$.players.length()").value(3))
                .andExpect(jsonPath("$.players[0].playerId").value(keeper.toString()))
                .andExpect(jsonPath("$.players[0].position").value("GOALKEEPER"))
                .andExpect(jsonPath("$.players[1].playerId").value(defender.toString()))
                .andExpect(jsonPath("$.players[2].playerId").value(midfielder.toString()))
                .andExpect(jsonPath("$.players[0].myRating").doesNotExist())
                .andExpect(jsonPath("$.players[0].votes").doesNotExist())
                .andExpect(jsonPath("$.players[0].average").doesNotExist())
                .andExpect(jsonPath("$.players[0].ratingPoints").doesNotExist());
    }

    /** RF-21: the votes are anonymous, also to the admin; only the result is published, once the window closes. */
    @Test
    @DisplayName("Nobody sees another member's votes, not even the admin, during the window or after it")
    void nobodySeesAnotherMembersVotes() throws Exception {
        Instant closesAt = closeMatch1();

        save(ana, match1, Map.of(keeper, 4, defender, 1)).andExpect(status().isOk());
        save(bea, match1, Map.of(keeper, 2)).andExpect(status().isOk());
        save(admin, match1, Map.of(midfielder, 3)).andExpect(status().isOk());

        assertThat(myRatings(ratings(ana, league, match1))).containsExactly(4, 1, null);
        assertThat(myRatings(ratings(bea, league, match1))).containsExactly(2, null, null);
        assertThat(myRatings(ratings(admin, league, match1))).containsExactly(null, null, 3);
        for (UUID member : List.of(admin, ana, bea)) {
            players(ratings(member, league, match1)).forEach(player -> {
                assertThat(player.fieldNames()).toIterable().containsExactlyElementsOf(PLAYER_FIELDS);
                assertThat(player.get("votes").isNull()).as("no result during the window").isTrue();
                assertThat(player.get("average").isNull()).isTrue();
                assertThat(player.get("ratingPoints").isNull()).isTrue();
            });
        }

        runJobAt(closesAt);

        assertThat(myRatings(ratings(ana, league, match1))).containsExactly(4, 1, null);
        assertThat(myRatings(ratings(bea, league, match1))).containsExactly(2, null, null);
        assertThat(myRatings(ratings(admin, league, match1))).containsExactly(null, null, 3);
        // spades: the goalkeeper 4 and 2 (3 → 10), the defender 1 (2), the midfielder 3 (10)
        ratings(bea, league, match1)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.players[0].votes").value(2))
                .andExpect(jsonPath("$.players[0].average").value(3.0))
                .andExpect(jsonPath("$.players[0].ratingPoints").value(10))
                .andExpect(jsonPath("$.players[1].votes").value(1))
                .andExpect(jsonPath("$.players[1].average").value(1.0))
                .andExpect(jsonPath("$.players[1].ratingPoints").value(2))
                .andExpect(jsonPath("$.players[2].votes").value(1))
                .andExpect(jsonPath("$.players[2].ratingPoints").value(10));
        players(ratings(admin, league, match1)).forEach(player ->
                assertThat(player.fieldNames()).toIterable().containsExactlyElementsOf(PLAYER_FIELDS));
    }

    @Test
    @DisplayName("Saving replaces all the member's votes: changed, new and removed ones, also all of them")
    void savingReplacesAllTheMembersVotes() throws Exception {
        closeMatch1();
        save(ana, match1, Map.of(keeper, 4, defender, 1)).andExpect(status().isOk());
        save(bea, match1, Map.of(keeper, 2)).andExpect(status().isOk());

        assertThat(myRatings(save(ana, match1, Map.of(defender, 2, midfielder, 0)))).containsExactly(null, 2, 0);

        assertThat(myRatings(ratings(ana, league, match1))).containsExactly(null, 2, 0);
        assertThat(storedVotes(ana)).containsOnly(entry(defender, 2), entry(midfielder, 0));
        assertThat(storedVotes(bea)).as("another member's votes are untouched").containsOnly(entry(keeper, 2));

        save(ana, match1, Map.of()).andExpect(status().isOk());

        assertThat(myRatings(ratings(ana, league, match1))).containsExactly(null, null, null);
        assertThat(storedVotes(ana)).isEmpty();
        assertThat(storedVotes(bea)).containsOnly(entry(keeper, 2));
    }

    @Test
    @DisplayName("Saving the same vote again keeps its time; changing it updates the time")
    void savingTheSameVoteAgainKeepsItsTime() throws Exception {
        closeMatch1();
        Instant firstSave = clock.now;
        save(ana, match1, Map.of(keeper, 4, defender, 1)).andExpect(status().isOk());

        clock.now = firstSave.plus(Duration.ofHours(1));
        save(ana, match1, Map.of(keeper, 4, defender, 2)).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select updated_at from rating_vote where player_id = ?", Timestamp.class,
                keeper)).isEqualTo(Timestamp.from(firstSave));
        assertThat(jdbc.queryForObject("select updated_at from rating_vote where player_id = ?", Timestamp.class,
                defender)).isEqualTo(Timestamp.from(clock.now));
    }

    @Test
    @DisplayName("Invalid ratings are answered with 400 and a code, and nothing is saved")
    void invalidRatingsAre400WithACode() throws Exception {
        closeMatch1();
        save(ana, match1, Map.of(keeper, 3)).andExpect(status().isOk());

        Map<String, String> invalid = Map.of(
                body(Map.of(keeper, 5)), "INVALID_RATING",
                body(Map.of(keeper, -1)), "INVALID_RATING",
                // the forward did not play
                body(Map.of(forward, 2)), "PLAYER_NOT_IN_SHEET",
                body(Map.of(UUID.randomUUID(), 2)), "PLAYER_NOT_IN_SHEET",
                "{\"ratings\":[{\"playerId\":\"%s\",\"value\":1},{\"playerId\":\"%1$s\",\"value\":2}]}"
                        .formatted(defender), "DUPLICATE_PLAYER");
        for (Map.Entry<String, String> entry : invalid.entrySet()) {
            save(ana, league, match1, entry.getKey())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(entry.getValue()));
        }
        for (String request : List.of("{}", "{\"ratings\":null}", "{\"ratings\":[null]}",
                "{\"ratings\":[{\"playerId\":null,\"value\":2}]}",
                "{\"ratings\":[{\"playerId\":\"%s\"}]}".formatted(keeper),
                "{\"ratings\":[{\"playerId\":\"%s\",\"value\":2.5}]}".formatted(keeper),
                "{\"ratings\":[{\"playerId\":\"not-a-uuid\",\"value\":2}]}")) {
            save(ana, league, match1, request).andExpect(status().isBadRequest());
        }

        assertThat(storedVotes(ana)).containsOnly(entry(keeper, 3));
    }

    @Test
    @DisplayName("Votes follow the format frozen in the window: 0 to 10 points although the league is back to spades")
    void votesFollowTheFormatFrozenInTheWindow() throws Exception {
        fixtures.settings(peña, settings -> settings.ratingFormat = RatingFormat.DECIMAL);
        closeMatch1();
        fixtures.settings(peña, settings -> settings.ratingFormat = RatingFormat.SPADES);

        save(ana, match1, Map.of(keeper, 10, defender, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("DECIMAL"));
        save(ana, match1, Map.of(keeper, 11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RATING"));

        assertThat(storedVotes(ana)).containsOnly(entry(keeper, 10), entry(defender, 0));
    }

    /** RF-17: the deadline is the server's clock, not the job. */
    @Test
    @DisplayName("Votes are accepted just before closesAt and rejected from it with 409, before the job and after")
    void votesAreRejectedFromClosesAtBeforeTheJobAndAfter() throws Exception {
        Instant closesAt = closeMatch1();

        clock.now = closesAt.minusSeconds(1);
        save(ana, match1, Map.of(keeper, 3)).andExpect(status().isOk());

        clock.now = closesAt;
        save(ana, match1, Map.of(keeper, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));
        save(bea, match1, Map.of(keeper, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));
        // closed for the members, but the result is not there until the job closes it
        ratings(ana, league, match1)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.players[0].myRating").value(3))
                .andExpect(jsonPath("$.players[0].votes").doesNotExist());

        runJobAt(closesAt.plus(Duration.ofMinutes(1)));

        save(ana, match1, Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_WINDOW_CLOSED"));
        ratings(ana, league, match1).andExpect(jsonPath("$.players[0].votes").value(1));
        assertThat(storedVotes(ana)).containsOnly(entry(keeper, 3));
        assertThat(storedVotes(bea)).isEmpty();
    }

    @Test
    @DisplayName("A match of the league not closed yet is answered with 409 MATCH_NOT_CLOSED")
    void aMatchNotClosedYetIs409() throws Exception {
        ratings(ana, league, match2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_CLOSED"));
        save(ana, match2, Map.of(keeper, 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_CLOSED"));

        assertThat(votes()).isZero();
    }

    @Test
    @DisplayName("A match of another league, closed or not, or an unknown one is answered with 404")
    void aMatchOfAnotherLeagueIs404() throws Exception {
        UUID otherPlayer = fixtures.player(other, GOALKEEPER);
        UUID otherClosed = fixtures.schedule(other, 1, KICKOFF_1);
        UUID otherScheduled = fixtures.schedule(other, 2, KICKOFF_2);
        close(other, otherClosed, KICKOFF_1, List.of(otherPlayer));

        for (UUID matchId : List.of(otherClosed, otherScheduled, UUID.randomUUID())) {
            ratings(ana, league, matchId).andExpect(status().isNotFound());
            save(ana, matchId, Map.of(otherPlayer, 2)).andExpect(status().isNotFound());
        }

        assertThat(votes()).isZero();
    }

    @Test
    @DisplayName("Non-members can neither rate nor see the ratings, also of a closed match")
    void nonMembersCanNeitherRateNorSeeTheRatings() throws Exception {
        closeMatch1();

        save(outsider, match1, Map.of(keeper, 2)).andExpect(status().isForbidden());
        ratings(outsider, league, match1).andExpect(status().isForbidden());
        windows(outsider).andExpect(status().isForbidden());

        assertThat(votes()).isZero();
    }

    @Test
    @DisplayName("The listing shows the open windows first, then the latest kickoff, with how many players I rated")
    void theListingShowsTheOpenWindowsFirst() throws Exception {
        windows(ana).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // match 1 with a long window, match 2 with a short one that is over first
        fixtures.settings(peña, settings -> settings.ratingWindowHours = 240);
        Instant closes1At = close(peña, match1, KICKOFF_1, List.of(keeper, defender, midfielder))
                .plus(Duration.ofHours(240));
        save(ana, match1, Map.of(keeper, 3, defender, 2)).andExpect(status().isOk());
        save(bea, match1, Map.of(keeper, 1)).andExpect(status().isOk());
        fixtures.settings(peña, settings -> settings.ratingWindowHours = 1);
        Instant closes2At = close(peña, match2, KICKOFF_2, squad).plus(Duration.ofHours(1));
        save(ana, match2, Map.of(forward, 4)).andExpect(status().isOk());
        clock.now = closes2At.plus(Duration.ofHours(1));

        windows(ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].matchId").value(match1.toString()))
                .andExpect(jsonPath("$[0].matchday").value(1))
                .andExpect(jsonPath("$[0].kickoffAt").value(KICKOFF_1.toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].closesAt").value(closes1At.toString()))
                .andExpect(jsonPath("$[0].players").value(3))
                .andExpect(jsonPath("$[0].myRatings").value(2))
                .andExpect(jsonPath("$[1].matchId").value(match2.toString()))
                .andExpect(jsonPath("$[1].status").value("CLOSED"))
                .andExpect(jsonPath("$[1].closesAt").value(closes2At.toString()))
                .andExpect(jsonPath("$[1].players").value(4))
                .andExpect(jsonPath("$[1].myRatings").value(1));
        windows(admin)
                .andExpect(jsonPath("$[0].myRatings").value(0))
                .andExpect(jsonPath("$[1].myRatings").value(0));
        mvc.perform(get("/api/leagues/{id}/ratings", other.id()).with(as(outsider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
