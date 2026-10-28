package com.buddyfantasy.scoring;

import java.util.UUID;

/**
 * A manager's points in a match as published in {@link MatchdayScored}: the sum of the players of their locked
 * lineup, or 0 if it was {@code overBudget} (RF-29, RF-31).
 */
public record ManagerScoreData(UUID userId, int points, boolean overBudget) {
}
