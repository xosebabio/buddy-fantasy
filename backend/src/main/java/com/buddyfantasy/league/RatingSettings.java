package com.buddyfantasy.league;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.RatingFormat;

/**
 * Rating parameters of a league (RF-17 to RF-23), as seen by other modules. {@code factor} and {@code base} are the
 * conversion of the current {@code format}: {@code points = factor × average + base}. With fewer than
 * {@code minVotes} votes a player counts as unrated: {@code defaultRating} converted the same way, or 0 points when
 * it is null.
 */
public record RatingSettings(
        int windowHours,
        RatingFormat format,
        BigDecimal factor,
        BigDecimal base,
        int minVotes,
        BigDecimal defaultRating) {
}
