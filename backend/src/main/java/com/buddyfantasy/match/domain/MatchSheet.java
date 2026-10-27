package com.buddyfantasy.match.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Result plus who played and their stats (RF-13, RF-14), always saved as a whole. While the match is scheduled it
 * may be a draft: {@code result} is {@code null} until known and there may be no participants yet. Performances are
 * kept sorted by player id, so two sheets with the same lines are equal whatever order they were sent in. The
 * configurable bounds (players, goals, stats) are checked by {@link MatchLimits} when the sheet is recorded.
 */
public record MatchSheet(MatchResult result, List<PlayerPerformance> performances) {

    public static final MatchSheet EMPTY = new MatchSheet(null, List.of());

    public MatchSheet {
        if (performances == null) {
            throw new DomainValidationException("Performances are required");
        }
        Set<UUID> players = new HashSet<>();
        for (PlayerPerformance performance : performances) {
            if (performance == null) {
                throw new DomainValidationException("Performances must not contain nulls");
            }
            if (!players.add(performance.playerId())) {
                throw new DomainValidationException("A player appears twice in the match sheet");
            }
        }
        List<PlayerPerformance> sorted = new ArrayList<>(performances);
        sorted.sort(Comparator.comparing(PlayerPerformance::playerId));
        performances = List.copyOf(sorted);
    }

    /** A closed match needs a result and at least one player. */
    public boolean isComplete() {
        return result != null && !performances.isEmpty();
    }

    /** Warnings never throw; a sheet without result has none. Only our own players' goals are counted. */
    public List<MatchSheetWarning> warnings() {
        if (result == null) {
            return List.of();
        }
        List<MatchSheetWarning> warnings = new ArrayList<>();
        int goals = performances.stream().mapToInt(p -> p.goals() + p.penaltyGoals()).sum();
        if (goals != result.goalsFor()) {
            warnings.add(MatchSheetWarning.GOALS_MISMATCH);
        }
        int ownGoals = performances.stream().mapToInt(PlayerPerformance::ownGoals).sum();
        if (ownGoals > result.goalsAgainst()) {
            warnings.add(MatchSheetWarning.OWN_GOALS_EXCEED_GOALS_AGAINST);
        }
        return List.copyOf(warnings);
    }

    public List<PlayerPerformanceData> performancesData() {
        return performances.stream().map(PlayerPerformance::toData).toList();
    }
}
