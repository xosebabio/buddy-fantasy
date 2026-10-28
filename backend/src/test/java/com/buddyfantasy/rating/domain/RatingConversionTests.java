package com.buddyfantasy.rating.domain;

import java.math.BigDecimal;
import java.util.List;

import com.buddyfantasy.datadefinition.RatingFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** From votes to points for ratings (RF-22, RF-23, RNF-11), with the defaults of the real {@code application.yml}. */
class RatingConversionTests {

    static final RatingRules SPADES = TestRatingSettings.rules(TestRatingSettings.defaults());
    static final RatingRules DECIMAL = TestRatingSettings.rules(TestRatingSettings.decimal());

    static int points(RatingRules rules, Integer... votes) {
        return RatingConversion.result(rules, List.of(votes)).ratingPoints();
    }

    @Test
    @DisplayName("The defaults rate in spades with 4 × average − 2, a minimum of 1 vote and no default rating")
    void theDefaultsMatchTheRequirements() {
        assertThat(SPADES.format()).isEqualTo(RatingFormat.SPADES);
        assertThat(SPADES.factor()).isEqualByComparingTo("4");
        assertThat(SPADES.base()).isEqualByComparingTo("-2");
        assertThat(SPADES.minVotes()).isEqualTo(1);
        assertThat(SPADES.defaultRating()).isNull();
        assertThat(DECIMAL.factor()).isEqualByComparingTo("1");
        assertThat(DECIMAL.base()).isEqualByComparingTo("0");
    }

    @ParameterizedTest(name = "{0} spades → {1} points")
    @CsvSource({"0, -2", "1, 2", "2, 6", "3, 10", "4, 14"})
    @DisplayName("Each average of spades is worth its points: 0 = −2, 1 = 2, 2 = 6, 3 = 10, 4 = 14")
    void eachSpadeIsWorthItsPoints(int spades, int expected) {
        assertThat(points(SPADES, spades)).isEqualTo(expected);
        assertThat(points(SPADES, spades, spades, spades)).isEqualTo(expected);
    }

    @Test
    @DisplayName("The exact average is converted and only the result is rounded, halves away from zero")
    void theExactAverageIsConvertedAndOnlyTheResultIsRounded() {
        assertThat(points(SPADES, 2, 3)).as("2.5 → 8").isEqualTo(8);
        assertThat(points(SPADES, 2, 3, 3, 2, 3)).as("2.6 → 8.4").isEqualTo(8);
        assertThat(points(SPADES, 1, 1, 2)).as("1.33… → 3.33").isEqualTo(3);
        assertThat(points(SPADES, 1, 0, 0, 0, 0, 0, 0, 0)).as("0.125 → −1.5").isEqualTo(-2);
        assertThat(points(SPADES, 1, 1, 1, 0, 0, 0, 0, 0)).as("0.375 → −0.5").isEqualTo(-1);
        assertThat(points(SPADES, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0)).as("0.2 → −1.2").isEqualTo(-1);
    }

    @Test
    @DisplayName("Decimal points are worth 1 point per point of average, rounded half up")
    void decimalPointsAreWorthOnePointPerPointOfAverage() {
        assertThat(points(DECIMAL, 7, 8)).as("7.5").isEqualTo(8);
        assertThat(points(DECIMAL, 7, 7, 7, 8, 8)).as("7.4").isEqualTo(7);
        assertThat(points(DECIMAL, 8, 9)).as("8.5").isEqualTo(9);
        assertThat(points(DECIMAL, 0)).isZero();
        assertThat(points(DECIMAL, 10)).isEqualTo(10);
    }

    @Test
    @DisplayName("An edited conversion is applied to the exact average, not to a rounded one")
    void anEditedConversionIsAppliedToTheExactAverage() {
        RatingRules rules = TestRatingSettings.rules(
                TestRatingSettings.with(TestRatingSettings.decimal(), "1.5", "-0.5", 1, null));

        // 1.5 × 7.5 − 0.5 = 10.75; rounding the average to 8 first would give 11.5 → 12.
        assertThat(points(rules, 7, 8)).isEqualTo(11);
    }

    @Test
    @DisplayName("Without votes a player scores 0 points, not the −2 of 0 spades")
    void withoutVotesAPlayerScoresZero() {
        PlayerRatingResult result = RatingConversion.result(SPADES, List.of());

        assertThat(result.ratingPoints()).isZero();
        assertThat(result.votes()).isZero();
        assertThat(result.average()).isNull();
    }

    @Test
    @DisplayName("With fewer votes than the minimum a player counts as unrated, with his average shown")
    void withFewerVotesThanTheMinimumAPlayerCountsAsUnrated() {
        RatingRules minTwo = TestRatingSettings.rules(
                TestRatingSettings.with(TestRatingSettings.defaults(), "4", "-2", 2, null));

        PlayerRatingResult result = RatingConversion.result(minTwo, List.of(4));

        assertThat(result.ratingPoints()).isZero();
        assertThat(result.votes()).isEqualTo(1);
        assertThat(result.average()).isEqualByComparingTo("4");
        assertThat(points(minTwo, 4, 3)).isEqualTo(12);
    }

    @Test
    @DisplayName("An unrated player scores the default rating converted the same way, if there is one")
    void anUnratedPlayerScoresTheDefaultRatingConverted() {
        RatingRules withDefault = TestRatingSettings.rules(
                TestRatingSettings.with(TestRatingSettings.defaults(), "4", "-2", 2, "1.5"));

        assertThat(points(withDefault)).as("4 × 1.5 − 2").isEqualTo(4);
        assertThat(points(withDefault, 4)).isEqualTo(4);
        assertThat(points(withDefault, 0, 0)).isEqualTo(-2);
    }

    @Test
    @DisplayName("The average is rounded to 2 decimals only to show it")
    void theAverageIsRoundedToTwoDecimalsOnlyToShowIt() {
        PlayerRatingResult result = RatingConversion.result(SPADES, List.of(1, 1, 2));

        assertThat(result.average()).isEqualTo(new BigDecimal("1.33"));
        assertThat(RatingConversion.result(SPADES, List.of(2, 3)).average()).isEqualTo(new BigDecimal("2.50"));
    }
}
