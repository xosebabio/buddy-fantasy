package com.buddyfantasy.league.adapter.out.config;

import java.math.BigDecimal;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.datadefinition.ScoringAction;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code league.defaults} in {@code application.yml}: parameters and scoring table of a new league. Validated by
 * the domain when {@link PropertiesLeagueDefaultsRepository} is created, so invalid values stop the application.
 * {@code rating.defaultRating} is optional (no default rating); {@code rating.conversion} has the conversion of
 * each format.
 */
@ConfigurationProperties("league.defaults")
record LeagueDefaultsProperties(
        Rating rating,
        Lineup lineup,
        Market market,
        Map<ScoringAction, Map<Position, Integer>> scoringRules) {

    record Rating(int windowHours, RatingFormat format, Conversions conversion, int minVotes,
                  BigDecimal defaultRating) {
    }

    record Conversions(Conversion spades, Conversion decimal) {
    }

    /** {@code points = factor × average + base}. */
    record Conversion(BigDecimal factor, BigDecimal base) {
    }

    record Lineup(int size, int lockHours, int maxChanges, boolean formationEnabled, int minGoalkeepers,
                  int minDefenders, int minMidfielders, int minForwards, boolean budgetEnabled, BigDecimal budget) {
    }

    record Market(BigDecimal k, BigDecimal maxChangePct, BigDecimal minValue) {
    }
}
