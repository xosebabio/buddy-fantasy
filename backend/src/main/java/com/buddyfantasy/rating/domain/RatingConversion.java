package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import lombok.experimental.UtilityClass;

/**
 * From a player's votes to his points for ratings (RF-22, RF-23). With at least {@code minVotes} votes:
 * {@code factor × average + base}, with the exact average (sum / votes, no intermediate rounding) and the result
 * rounded to a whole number, halves away from zero (8.5 → 9, −1.5 → −2). With fewer, the player counts as
 * unrated: the default rating converted the same way or, without it, 0 points (not the −2 of 0 spades).
 */
@UtilityClass
public class RatingConversion {

    /** Decimals of the average, only to show it. */
    private final int AVERAGE_SCALE = 2;

    public PlayerRatingResult result(RatingRules rules, List<Integer> votes) {
        int count = votes.size();
        BigDecimal sum = BigDecimal.valueOf(votes.stream().mapToInt(Integer::intValue).sum());
        BigDecimal average = count == 0 ? null
                : sum.divide(BigDecimal.valueOf(count), AVERAGE_SCALE, RoundingMode.HALF_UP);
        return new PlayerRatingResult(count, average, points(rules, sum, count));
    }

    private int points(RatingRules rules, BigDecimal sum, int count) {
        if (count >= rules.minVotes()) {
            // (factor × sum + base × count) / count is factor × average + base, rounded once.
            return rules.factor().multiply(sum).add(rules.base().multiply(BigDecimal.valueOf(count)))
                    .divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                    .intValueExact();
        }
        if (rules.defaultRating() == null) {
            return 0;
        }
        return rules.factor().multiply(rules.defaultRating()).add(rules.base())
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
