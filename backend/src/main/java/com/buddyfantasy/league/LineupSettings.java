package com.buddyfantasy.league;

import java.math.BigDecimal;

/**
 * Lineup parameters of a league (RF-24 to RF-29), as seen by other modules. The per-position minimums only apply
 * when {@code formationEnabled}; {@code budget} (in millions) only when {@code budgetEnabled}.
 */
public record LineupSettings(
        int size,
        int lockHours,
        int maxChanges,
        boolean formationEnabled,
        int minGoalkeepers,
        int minDefenders,
        int minMidfielders,
        int minForwards,
        boolean budgetEnabled,
        BigDecimal budget) {
}
