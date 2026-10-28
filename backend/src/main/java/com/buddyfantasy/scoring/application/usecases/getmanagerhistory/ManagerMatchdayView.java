package com.buddyfantasy.scoring.application.usecases.getmanagerhistory;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.domain.ManagerMatchday;

/**
 * A scored match of a manager's history (RF-33): their points, 0 without a locked lineup or over budget (RF-29), and
 * each player of the lineup, sorted by points and then by id; empty without one. {@code inStandings} is false for a
 * match before they last joined the league, which no longer counts for them.
 */
public record ManagerMatchdayView(UUID matchId, int matchday, Instant kickoffAt, int points, boolean overBudget,
                                  boolean inStandings, List<LinedUpPlayerView> players) {

    static ManagerMatchdayView from(ManagerMatchday matchday, boolean inStandings) {
        boolean hasLineup = matchday.lineup() != null;
        return new ManagerMatchdayView(matchday.matchId(), matchday.matchday(), matchday.kickoffAt(),
                matchday.points(), hasLineup && matchday.lineup().overBudget(), inStandings,
                hasLineup ? matchday.lineup().playerIds().stream()
                        .map(playerId -> LinedUpPlayerView.from(playerId, matchday.lines().get(playerId),
                                matchday.rules()))
                        .sorted(Comparator.comparingInt(LinedUpPlayerView::points).reversed()
                                .thenComparing(LinedUpPlayerView::id))
                        .toList() : List.of());
    }
}
