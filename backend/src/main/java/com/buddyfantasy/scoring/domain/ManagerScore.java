package com.buddyfantasy.scoring.domain;

import java.util.UUID;

import com.buddyfantasy.scoring.ManagerScoreData;

/** A manager's points in a match (RF-31): those of their locked lineup's players, or 0 if over budget. */
public record ManagerScore(UUID userId, int points, boolean overBudget) {

    public ManagerScoreData toData() {
        return new ManagerScoreData(userId, points, overBudget);
    }
}
