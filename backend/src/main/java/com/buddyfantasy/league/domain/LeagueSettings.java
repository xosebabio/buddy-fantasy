package com.buddyfantasy.league.domain;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * Per-league parameters (RF-07). Money values are in millions. {@code defaultRating} is nullable
 * (no default rating for players without votes). The rating conversion is linear per format,
 * {@code points = factor × average + base} (RF-22), and both are kept so switching format does not lose the other.
 * The per-position minimums only apply when {@code lineupFormationEnabled} (RF-24). A new league is seeded from
 * {@code league.defaults} (see {@code LeagueDefaultsRepository}); the configurable upper bounds are in
 * {@link LeagueLimits}.
 */
public record LeagueSettings(
        int ratingWindowHours,
        RatingFormat ratingFormat,
        BigDecimal ratingSpadesFactor,
        BigDecimal ratingSpadesBase,
        BigDecimal ratingDecimalFactor,
        BigDecimal ratingDecimalBase,
        int ratingMinVotes,
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
    /** The rating factors and bases are stored as {@code NUMERIC(6, 2)}. */
    private static final int CONVERSION_INTEGER_DIGITS = 4;
    private static final int CONVERSION_SCALE = 2;

    public LeagueSettings {
        require(ratingWindowHours >= 1, LeagueError.INVALID_RATING_WINDOW_HOURS, "ratingWindowHours must be >= 1");
        require(ratingFormat != null, LeagueError.INVALID_RATING_FORMAT, "ratingFormat is required");
        require(isPositive(ratingSpadesFactor) && fitsConversion(ratingSpadesFactor),
                LeagueError.INVALID_RATING_CONVERSION,
                "ratingSpadesFactor must be > 0 with up to 4 integer digits and 2 decimals");
        require(fitsConversion(ratingSpadesBase), LeagueError.INVALID_RATING_CONVERSION,
                "ratingSpadesBase is required, with up to 4 integer digits and 2 decimals");
        require(isPositive(ratingDecimalFactor) && fitsConversion(ratingDecimalFactor),
                LeagueError.INVALID_RATING_CONVERSION,
                "ratingDecimalFactor must be > 0 with up to 4 integer digits and 2 decimals");
        require(fitsConversion(ratingDecimalBase), LeagueError.INVALID_RATING_CONVERSION,
                "ratingDecimalBase is required, with up to 4 integer digits and 2 decimals");
        require(ratingMinVotes >= 1, LeagueError.INVALID_RATING_MIN_VOTES, "ratingMinVotes must be >= 1");
        require(defaultRating == null
                        || (defaultRating.signum() >= 0
                        && defaultRating.compareTo(BigDecimal.valueOf(ratingFormat.maxRating())) <= 0),
                LeagueError.INVALID_DEFAULT_RATING, "defaultRating must be between 0 and " + ratingFormat.maxRating());
        require(lineupLockHours >= 0, LeagueError.INVALID_LINEUP_LOCK_HOURS, "lineupLockHours must be >= 0");
        require(lineupMaxChanges >= 0, LeagueError.INVALID_LINEUP_MAX_CHANGES, "lineupMaxChanges must be >= 0");
        require(lineupSize >= 1, LeagueError.INVALID_LINEUP_SIZE, "lineupSize must be >= 1");
        require(lineupMinGoalkeepers >= 0 && lineupMinDefenders >= 0 && lineupMinMidfielders >= 0
                && lineupMinForwards >= 0,
                LeagueError.INVALID_LINEUP_MINIMUMS, "lineup minimums per position must be >= 0");
        require(!lineupFormationEnabled
                        || lineupMinGoalkeepers + lineupMinDefenders + lineupMinMidfielders + lineupMinForwards
                        <= lineupSize,
                LeagueError.INVALID_LINEUP_MINIMUMS,
                "lineup minimums per position cannot add up to more than lineupSize");
        require(isPositive(lineupBudget), LeagueError.INVALID_LINEUP_BUDGET, "lineupBudget must be > 0");
        require(isPositive(marketK), LeagueError.INVALID_MARKET_K, "marketK must be > 0");
        require(isPositive(marketMaxChangePct) && marketMaxChangePct.compareTo(HUNDRED) <= 0,
                LeagueError.INVALID_MARKET_MAX_CHANGE, "marketMaxChangePct must be in (0, 100]");
        require(isPositive(marketMinValue), LeagueError.INVALID_MARKET_MIN_VALUE, "marketMinValue must be > 0");
    }

    /** The conversion factor of the current {@link #ratingFormat()}. */
    public BigDecimal ratingFactor() {
        return switch (ratingFormat) {
            case SPADES -> ratingSpadesFactor;
            case DECIMAL -> ratingDecimalFactor;
        };
    }

    /** The conversion base of the current {@link #ratingFormat()}. */
    public BigDecimal ratingBase() {
        return switch (ratingFormat) {
            case SPADES -> ratingSpadesBase;
            case DECIMAL -> ratingDecimalBase;
        };
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean fitsConversion(BigDecimal value) {
        if (value == null) {
            return false;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() <= CONVERSION_SCALE
                && normalized.precision() - normalized.scale() <= CONVERSION_INTEGER_DIGITS;
    }

    private static void require(boolean condition, LeagueError code, String message) {
        if (!condition) {
            throw new DomainValidationException(code, message);
        }
    }
}
