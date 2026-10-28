package com.buddyfantasy.scoring.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/** Why a scoring view cannot be shown. */
public enum ScoringError implements ErrorCode {
    /** The match is of the league but has no points yet (409): not closed, or its lineups not locked. */
    MATCH_NOT_SCORED
}
