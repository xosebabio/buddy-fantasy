package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * The league's rating parameters frozen when a window opens, used until the end, corrections included: the
 * {@code format} of the votes (RF-19), the conversion {@code points = factor × average + base} (RF-22), and the
 * minimum of votes below which a player counts as unrated, worth {@code defaultRating} converted the same way or 0
 * points when it is null (RF-23).
 */
public record RatingRules(RatingFormat format, BigDecimal factor, BigDecimal base, int minVotes,
        BigDecimal defaultRating) {

    public RatingRules {
        require(format != null, "The rating format is required");
        require(factor != null && factor.signum() > 0, "The rating conversion factor must be > 0");
        require(base != null, "The rating conversion base is required");
        require(minVotes >= 1, "The minimum of votes must be >= 1");
        require(defaultRating == null || (defaultRating.signum() >= 0
                        && defaultRating.compareTo(BigDecimal.valueOf(format.maxRating())) <= 0),
                "The default rating must be between 0 and " + format.maxRating());
    }

    public static RatingRules from(RatingSettings settings) {
        return new RatingRules(settings.format(), settings.factor(), settings.base(), settings.minVotes(),
                settings.defaultRating());
    }

    /** A whole value from 0 to the format's maximum; otherwise {@link RatingError#INVALID_RATING}. */
    public void requireValidVote(int value) {
        if (value < 0 || value > format.maxRating()) {
            throw new DomainValidationException(RatingError.INVALID_RATING,
                    "A rating in " + format + " is between 0 and " + format.maxRating());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new DomainValidationException(message);
        }
    }
}
