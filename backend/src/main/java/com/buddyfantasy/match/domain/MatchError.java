package com.buddyfantasy.match.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/**
 * Why a match or its sheet cannot be saved, closed or deleted. {@link #MATCHDAY_TAKEN}, {@link #KICKOFF_TAKEN},
 * {@link #MATCH_CLOSED}, {@link #MATCH_NOT_STARTED} and {@link #MATCH_SHEET_INCOMPLETE} are 409; the rest, 400.
 */
public enum MatchError implements ErrorCode {
    /** Another match of the league has that matchday: 1 matchday = 1 match. */
    MATCHDAY_TAKEN,
    /** Another match of the league starts at the same time. */
    KICKOFF_TAKEN,
    /** A closed match cannot change its matchday or kickoff, nor be deleted. */
    MATCH_CLOSED,
    /** A match cannot be closed before kickoff. */
    MATCH_NOT_STARTED,
    /** Closing, or correcting a closed match, needs a result and at least one player. */
    MATCH_SHEET_INCOMPLETE,
    /** Blank or longer than {@code match.limits.opponent-max-length}. */
    INVALID_OPPONENT,
    /** Below {@code match.limits.matchday-min} or beyond its storage range. */
    INVALID_MATCHDAY,
    /** Negative or above {@code match.limits.goals-max}. */
    INVALID_GOALS,
    /** A player's stat is negative or above {@code match.limits.stat-count-max}. */
    INVALID_STAT_COUNT,
    /** A double yellow together with the yellow card or the straight red. */
    INVALID_CARDS,
    /** More players than {@code match.limits.sheet-players-max}. */
    SHEET_TOO_LARGE,
    DUPLICATE_PLAYER,
    /** Unknown or from another league. */
    UNKNOWN_PLAYER,
    /** A retired player who was not in the sheet yet. */
    INACTIVE_PLAYER
}
