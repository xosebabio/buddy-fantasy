package com.buddyfantasy.league.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/**
 * Why a league, its parameters or a membership cannot be saved. {@link #ADMIN_CANNOT_LEAVE} is 409; the rest, 400,
 * each naming the parameter out of its range.
 */
public enum LeagueError implements ErrorCode {
    /** The league always keeps its admin: they can neither be removed nor leave. */
    ADMIN_CANNOT_LEAVE,
    /** Blank or longer than {@code league.limits.name-max-length}. */
    INVALID_LEAGUE_NAME,
    INVALID_RATING_WINDOW_HOURS,
    INVALID_RATING_FORMAT,
    /** A factor or base of the rating conversion. */
    INVALID_RATING_CONVERSION,
    INVALID_RATING_MIN_VOTES,
    INVALID_DEFAULT_RATING,
    /** Negative or above {@code league.limits.lineup-lock-hours-max}. */
    INVALID_LINEUP_LOCK_HOURS,
    INVALID_LINEUP_MAX_CHANGES,
    INVALID_LINEUP_SIZE,
    /** A negative minimum per position, or the minimums add up to more than the lineup size. */
    INVALID_LINEUP_MINIMUMS,
    INVALID_LINEUP_BUDGET,
    INVALID_MARKET_K,
    INVALID_MARKET_MAX_CHANGE,
    INVALID_MARKET_MIN_VALUE,
    /** Some action or position is missing from the scoring table. */
    SCORING_TABLE_INCOMPLETE
}
