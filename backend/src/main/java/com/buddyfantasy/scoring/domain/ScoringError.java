package com.buddyfantasy.scoring.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/** Why a scoring view cannot be shown. */
public enum ScoringError implements ErrorCode {
    /**
     * The match is of the league but has no points yet (409): not closed, its lineups not locked or its rating window
     * not closed.
     */
    MATCH_NOT_SCORED
}
