package com.buddyfantasy.lineup.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/**
 * Why a lineup cannot be saved, shown or locked. {@link #MAX_CHANGES_EXCEEDED}, {@link #LINEUPS_NOT_LOCKED} and
 * {@link #LOCK_NOT_DUE} are 409; the rest, 400.
 */
public enum LineupError implements ErrorCode {
    /** Not exactly {@code lineupSize} players. */
    SIZE,
    DUPLICATE_PLAYER,
    /** A retired player: he must be replaced, for free. */
    INACTIVE_PLAYER,
    /** Unknown or from another league. */
    UNKNOWN_PLAYER,
    /** Fewer players than the league's minimum in some position. */
    FORMATION,
    OVER_BUDGET,
    MAX_CHANGES_EXCEEDED,
    /** The lineups of the match are not visible until they are locked. */
    LINEUPS_NOT_LOCKED,
    /** The lineups of a match cannot be locked before its lock time; never reaches the API. */
    LOCK_NOT_DUE
}
