package com.buddyfantasy.rating;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A player's ratings in a match as published in {@link RatingWindowClosed} and {@link RatingsCorrected}: how many
 * votes counted, their average (null without votes; rounded to 2 decimals, only to show) and the points for ratings
 * (RF-22, RF-23), which {@code scoring} adds to the points for actions.
 */
public record PlayerRatingData(UUID playerId, int votes, BigDecimal average, int ratingPoints) {
}
