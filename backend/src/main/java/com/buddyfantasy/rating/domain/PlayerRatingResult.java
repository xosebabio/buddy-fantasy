package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.rating.PlayerRatingData;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * A player's ratings once the window closes ({@link RatingConversion}): the votes that counted, their average
 * (null without votes, rounded to 2 decimals only to show) and the points for ratings.
 */
public record PlayerRatingResult(int votes, BigDecimal average, int ratingPoints) {

    public PlayerRatingResult {
        if (votes < 0) {
            throw new DomainValidationException("The number of votes cannot be negative");
        }
        if ((average == null) != (votes == 0)) {
            throw new DomainValidationException("There is an average if and only if there are votes");
        }
    }

    public PlayerRatingData toData(UUID playerId) {
        return new PlayerRatingData(playerId, votes, average, ratingPoints);
    }
}
