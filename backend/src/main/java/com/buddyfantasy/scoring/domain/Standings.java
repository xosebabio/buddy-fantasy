package com.buddyfantasy.scoring.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import lombok.experimental.UtilityClass;

/**
 * The standings of a league (RF-32), only of its current members ({@code memberSince}, as the league module gives
 * it). A match counts for a member who already was one at kickoff: one who left and came back starts from zero, and
 * one who joined mid-season is not in the earlier matches. A member without a lineup scores 0 in a match. Sorted by
 * points; tied members share the position (1, 1, 3), with no tie-break.
 */
@UtilityClass
public class Standings {

    /** The standings of one scored match. */
    public List<MatchdayStanding> matchday(MatchdayPoints match, Map<UUID, Instant> memberSince) {
        List<MatchdayStanding> rows = memberSince.entrySet().stream()
                .filter(member -> match.countsFor(member.getValue()))
                .map(member -> new MatchdayStanding(0, member.getKey(), match.pointsOf(member.getKey())))
                .toList();
        return ranked(rows, MatchdayStanding::points, MatchdayStanding::userId,
                (position, row) -> new MatchdayStanding(position, row.userId(), row.points()));
    }

    /** The overall standings over every scored match of the league, in any order. */
    public List<OverallStanding> overall(Collection<MatchdayPoints> matches, Map<UUID, Instant> memberSince) {
        List<MatchdayPoints> chronological = matches.stream()
                .sorted(Comparator.comparing(MatchdayPoints::kickoffAt))
                .toList();
        List<OverallStanding> rows = memberSince.entrySet().stream()
                .map(member -> {
                    UUID userId = member.getKey();
                    List<MatchdayPoints> counted = chronological.stream()
                            .filter(match -> match.countsFor(member.getValue()))
                            .toList();
                    int points = counted.stream().mapToInt(match -> match.pointsOf(userId)).sum();
                    int last = counted.isEmpty() ? 0 : counted.getLast().pointsOf(userId);
                    return new OverallStanding(0, userId, points, counted.size(), last);
                })
                .toList();
        return ranked(rows, OverallStanding::points, OverallStanding::userId, (position, row) -> row.atPosition(position));
    }

    /** Sorts by points (then by user, only for a stable order) and gives tied rows the same position. */
    private <T> List<T> ranked(List<T> rows, ToIntFunction<T> points, Function<T, UUID> userId,
            BiFunction<Integer, T, T> atPosition) {
        List<T> sorted = rows.stream()
                .sorted(Comparator.comparingInt(points).reversed().thenComparing(userId))
                .toList();
        List<T> ranked = new ArrayList<>(sorted.size());
        int position = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (i == 0 || points.applyAsInt(sorted.get(i)) != points.applyAsInt(sorted.get(i - 1))) {
                position = i + 1;
            }
            ranked.add(atPosition.apply(position, sorted.get(i)));
        }
        return List.copyOf(ranked);
    }
}
