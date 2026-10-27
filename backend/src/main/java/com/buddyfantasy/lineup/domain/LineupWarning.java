package com.buddyfantasy.lineup.domain;

/**
 * Problems of a saved lineup caused by later changes (values, retirements, league parameters). They never block
 * anything: the lineup carries over as it is, and only {@link #OVER_BUDGET} at the lock makes it score 0 points.
 */
public enum LineupWarning {
    /** With the current values it no longer fits the budget. */
    OVER_BUDGET,
    /** Some player was retired; replacing him is free. */
    RETIRED_PLAYERS,
    /** The league changed {@code lineupSize}. */
    SIZE_MISMATCH
}
