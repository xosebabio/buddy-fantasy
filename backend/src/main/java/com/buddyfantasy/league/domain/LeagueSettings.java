package com.buddyfantasy.league.domain;

import java.math.BigDecimal;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Per-league parameters (RF-07). Money values are in millions. {@code defaultRating} is nullable
 * (no default rating for players without votes). The per-position minimums only apply when
 * {@code lineupFormationEnabled} (RF-24). A new league is seeded from {@code league.defaults}
 * (see {@code LeagueDefaultsRepository}); the configurable upper bounds are in {@link LeagueLimits}.
 */
public record LeagueSettings(
        int ratingWindowHours,
        RatingFormat ratingFormat,
        BigDecimal ratingPointsPerUnit,
        BigDecimal defaultRating,
        int lineupLockHours,
        int lineupMaxChanges,
        int lineupSize,
        boolean lineupFormationEnabled,
        int lineupMinGoalkeepers,
        int lineupMinDefenders,
        int lineupMinMidfielders,
        int lineupMinForwards,
        boolean lineupBudgetEnabled,
        BigDecimal lineupBudget,
        BigDecimal marketK,
        BigDecimal marketMaxChangePct,
        BigDecimal marketMinValue) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public LeagueSettings {
        require(ratingWindowHours >= 1, "ratingWindowHours must be >= 1");
        require(ratingFormat != null, "ratingFormat is required");
        require(isPositive(ratingPointsPerUnit), "ratingPointsPerUnit must be > 0");
        require(defaultRating == null
                        || (defaultRating.signum() >= 0 && defaultRating.compareTo(ratingFormat.maxRating()) <= 0),
                "defaultRating must be between 0 and " + ratingFormat.maxRating());
        require(lineupLockHours >= 0, "lineupLockHours must be >= 0");
        require(lineupMaxChanges >= 0, "lineupMaxChanges must be >= 0");
        require(lineupSize >= 1, "lineupSize must be >= 1");
        require(lineupMinGoalkeepers >= 0 && lineupMinDefenders >= 0 && lineupMinMidfielders >= 0
                && lineupMinForwards >= 0, "lineup minimums per position must be >= 0");
        require(!lineupFormationEnabled
                        || lineupMinGoalkeepers + lineupMinDefenders + lineupMinMidfielders + lineupMinForwards
                        <= lineupSize,
                "lineup minimums per position cannot add up to more than lineupSize");
        require(isPositive(lineupBudget), "lineupBudget must be > 0");
        require(isPositive(marketK), "marketK must be > 0");
        require(isPositive(marketMaxChangePct) && marketMaxChangePct.compareTo(HUNDRED) <= 0,
                "marketMaxChangePct must be in (0, 100]");
        require(isPositive(marketMinValue), "marketMinValue must be > 0");
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainValidationException(message);
        }
    }
}
