package com.buddyfantasy.league.domain;

import java.math.BigDecimal;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Per-league parameters (RF-07). Money values are in millions. {@code defaultRating} is nullable
 * (no default rating for players without votes).
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
        require(isPositive(lineupBudget), "lineupBudget must be > 0");
        require(isPositive(marketK), "marketK must be > 0");
        require(isPositive(marketMaxChangePct) && marketMaxChangePct.compareTo(HUNDRED) <= 0,
                "marketMaxChangePct must be in (0, 100]");
        require(isPositive(marketMinValue), "marketMinValue must be > 0");
    }

    public static LeagueSettings defaults() {
        return new LeagueSettings(24, RatingFormat.SPADES, BigDecimal.ONE, null, 2, 2, 7, false, false,
                new BigDecimal("100"), new BigDecimal("0.05"), new BigDecimal("10"), BigDecimal.ONE);
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
