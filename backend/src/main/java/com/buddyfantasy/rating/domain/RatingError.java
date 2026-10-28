package com.buddyfantasy.rating.domain;

import com.buddyfantasy.shared.domain.ErrorCode;

/**
 * Why ratings cannot be saved or shown. {@link #INVALID_RATING}, {@link #PLAYER_NOT_IN_SHEET} and
 * {@link #DUPLICATE_PLAYER} are 400; the rest, 409.
 */
public enum RatingError implements ErrorCode {
    /** Outside the window's format: 0 to 4 spades or 0 to 10 points. */
    INVALID_RATING,
    /** Only the players in the sheet of the match can be rated (RF-18). */
    PLAYER_NOT_IN_SHEET,
    DUPLICATE_PLAYER,
    /** The window is closed, or its time is up although the job has not closed it yet (RF-17). */
    RATING_WINDOW_CLOSED,
    /** The match is of the league but not closed yet: it has no window. */
    MATCH_NOT_CLOSED,
    /** The admin cancelled the member's ratings of the match (RF-41). */
    RATER_EXCLUDED
}
