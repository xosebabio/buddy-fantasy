package com.buddyfantasy.lineup.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/** Why a lineup cannot be saved. All are 400 except {@link #MAX_CHANGES_EXCEEDED} (409). */
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
    MAX_CHANGES_EXCEEDED
}
