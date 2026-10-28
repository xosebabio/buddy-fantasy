package com.buddyfantasy.scoring.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Standings of one match and overall (RF-32): current members only, since they joined, ties sharing the position. */
class StandingsTests {

    static final Instant SEASON = Instant.parse("2025-09-01T00:00:00Z");
    static final Instant FIRST = Instant.parse("2025-09-14T10:00:00Z");
    static final Instant SECOND = FIRST.plus(Duration.ofDays(7));
    static final Instant THIRD = SECOND.plus(Duration.ofDays(7));

    final UUID ana = UUID.randomUUID();
    final UUID brais = UUID.randomUUID();
    final UUID carla = UUID.randomUUID();
    final UUID dani = UUID.randomUUID();

    static MatchdayPoints match(Instant kickoffAt, Map<UUID, Integer> points) {
        return new MatchdayPoints(UUID.randomUUID(), kickoffAt, points);
    }

    @Test
    @DisplayName("A match is sorted by points and tied members share the position")
    void aMatchIsSortedByPointsAndTiedMembersShareThePosition() {
        MatchdayPoints match = match(FIRST, Map.of(ana, 10, brais, 14, carla, 10, dani, 3));

        List<MatchdayStanding> standings = Standings.matchday(match,
                Map.of(ana, SEASON, brais, SEASON, carla, SEASON, dani, SEASON));

        assertThat(standings).extracting(MatchdayStanding::position).containsExactly(1, 2, 2, 4);
        assertThat(standings).extracting(MatchdayStanding::points).containsExactly(14, 10, 10, 3);
        assertThat(standings.getFirst().userId()).isEqualTo(brais);
        assertThat(standings.getLast().userId()).isEqualTo(dani);
    }

    @Test
    @DisplayName("A member without a lineup scores zero in the match")
    void aMemberWithoutLineupScoresZeroInTheMatch() {
        MatchdayPoints match = match(FIRST, Map.of(ana, 5));

        List<MatchdayStanding> standings = Standings.matchday(match, Map.of(ana, SEASON, brais, SEASON));

        assertThat(standings).containsExactly(new MatchdayStanding(1, ana, 5), new MatchdayStanding(2, brais, 0));
    }

    @Test
    @DisplayName("Negative points rank below a member without a lineup")
    void negativePointsRankBelowAMemberWithoutLineup() {
        MatchdayPoints match = match(FIRST, Map.of(ana, -4));

        List<MatchdayStanding> standings = Standings.matchday(match, Map.of(ana, SEASON, brais, SEASON));

        assertThat(standings).containsExactly(new MatchdayStanding(1, brais, 0), new MatchdayStanding(2, ana, -4));
    }

    @Test
    @DisplayName("A match only lists current members who were already members at kickoff")
    void aMatchOnlyListsCurrentMembersWhoAlreadyWereOnesAtKickoff() {
        MatchdayPoints match = match(SECOND, Map.of(ana, 5, brais, 8, carla, 2));

        List<MatchdayStanding> standings = Standings.matchday(match,
                Map.of(ana, SEASON, carla, SECOND, dani, SECOND.plusSeconds(1)));

        assertThat(standings).containsExactly(new MatchdayStanding(1, ana, 5), new MatchdayStanding(2, carla, 2));
    }

    @Test
    @DisplayName("The overall standings add up the matches, count them and keep the last one in kickoff order")
    void overallAddsUpTheMatchesCountsThemAndKeepsTheLastOneInKickoffOrder() {
        List<MatchdayPoints> matches = List.of(
                match(THIRD, Map.of(ana, 4)),
                match(FIRST, Map.of(ana, 10, brais, 7)),
                match(SECOND, Map.of(ana, 6, brais, 12)));

        List<OverallStanding> standings = Standings.overall(matches, Map.of(ana, SEASON, brais, SEASON));

        assertThat(standings).containsExactly(new OverallStanding(1, ana, 20, 3, 4),
                new OverallStanding(2, brais, 19, 3, 0));
    }

    @Test
    @DisplayName("Overall ties share the position")
    void overallTiesShareThePosition() {
        List<MatchdayPoints> matches = List.of(match(FIRST, Map.of(ana, 9, brais, 9, carla, 1)));

        List<OverallStanding> standings = Standings.overall(matches,
                Map.of(ana, SEASON, brais, SEASON, carla, SEASON));

        assertThat(standings).extracting(OverallStanding::position).containsExactly(1, 1, 3);
    }

    @Test
    @DisplayName("The overall standings only count the matches since the member joined")
    void overallOnlyCountsTheMatchesSinceTheMemberJoined() {
        List<MatchdayPoints> matches = List.of(
                match(FIRST, Map.of(ana, 10, carla, 50)),
                match(SECOND, Map.of(ana, 5, carla, 3)),
                match(THIRD, Map.of(ana, 1, carla, 4)));
        Instant rejoined = FIRST.plus(Duration.ofDays(1));

        List<OverallStanding> standings = Standings.overall(matches, Map.of(ana, SEASON, carla, rejoined));

        assertThat(standings).containsExactly(new OverallStanding(1, ana, 16, 3, 1),
                new OverallStanding(2, carla, 7, 2, 4));
    }

    @Test
    @DisplayName("The overall standings leave out former members and list new ones with zero")
    void overallLeavesOutFormerMembersAndListsNewOnesWithZero() {
        List<MatchdayPoints> matches = List.of(match(FIRST, Map.of(ana, 10, brais, 30)));

        List<OverallStanding> standings = Standings.overall(matches,
                Map.of(ana, SEASON, dani, THIRD));

        assertThat(standings).containsExactly(new OverallStanding(1, ana, 10, 1, 10),
                new OverallStanding(2, dani, 0, 0, 0));
    }

    @Test
    @DisplayName("Without scored matches every member is first with zero")
    void withoutScoredMatchesEveryMemberIsFirstWithZero() {
        List<OverallStanding> standings = Standings.overall(List.of(), Map.of(ana, SEASON, brais, SEASON));

        assertThat(standings).extracting(OverallStanding::position).containsExactly(1, 1);
        assertThat(standings).extracting(OverallStanding::points).containsExactly(0, 0);
    }
}
