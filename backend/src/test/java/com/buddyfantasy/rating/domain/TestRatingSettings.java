package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.league.RatingSettings;
import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import com.buddyfantasy.league.domain.LeagueSettings;
import lombok.experimental.UtilityClass;

/** Rating parameters for unit tests: the ones in the real {@code application.yml}, and edited copies of them. */
@UtilityClass
class TestRatingSettings {

    /** The defaults: spades. */
    RatingSettings defaults() {
        return of(TestLeagueDefaults.settings());
    }

    /** The defaults switched to decimal points, with the default decimal conversion. */
    RatingSettings decimal() {
        LeagueSettings s = TestLeagueDefaults.settings();
        return new RatingSettings(s.ratingWindowHours(), RatingFormat.DECIMAL, s.ratingDecimalFactor(),
                s.ratingDecimalBase(), s.ratingMinVotes(), null);
    }

    /** {@code settings} with another conversion, minimum of votes and default rating. */
    RatingSettings with(RatingSettings settings, String factor, String base, int minVotes, String defaultRating) {
        return new RatingSettings(settings.windowHours(), settings.format(), new BigDecimal(factor),
                new BigDecimal(base), minVotes, defaultRating == null ? null : new BigDecimal(defaultRating));
    }

    RatingRules rules(RatingSettings settings) {
        return RatingRules.from(settings);
    }

    private RatingSettings of(LeagueSettings s) {
        return new RatingSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingFactor(), s.ratingBase(),
                s.ratingMinVotes(), s.defaultRating());
    }
}
