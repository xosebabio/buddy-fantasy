package com.buddyfantasy.league.domain;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueSettingsTests {

    private final LeagueSettings defaults = TestLeagueDefaults.settings();

    @Test
    @DisplayName("The defaults match the requirements")
    void defaultsMatchRequirements() {
        assertThat(defaults.ratingWindowHours()).isEqualTo(24);
        assertThat(defaults.ratingFormat()).isEqualTo(RatingFormat.SPADES);
        assertThat(defaults.ratingSpadesFactor()).isEqualByComparingTo("4");
        assertThat(defaults.ratingSpadesBase()).isEqualByComparingTo("-2");
        assertThat(defaults.ratingDecimalFactor()).isEqualByComparingTo("1");
        assertThat(defaults.ratingDecimalBase()).isEqualByComparingTo("0");
        assertThat(defaults.ratingMinVotes()).isEqualTo(1);
        assertThat(defaults.defaultRating()).isNull();
        assertThat(defaults.lineupLockHours()).isEqualTo(2);
        assertThat(defaults.lineupMaxChanges()).isEqualTo(2);
        assertThat(defaults.lineupSize()).isEqualTo(7);
        assertThat(defaults.lineupFormationEnabled()).isFalse();
        assertThat(defaults.lineupMinGoalkeepers()).isEqualTo(1);
        assertThat(defaults.lineupMinDefenders()).isEqualTo(1);
        assertThat(defaults.lineupMinMidfielders()).isEqualTo(1);
        assertThat(defaults.lineupMinForwards()).isEqualTo(1);
        assertThat(defaults.lineupBudgetEnabled()).isTrue();
        assertThat(defaults.lineupBudget()).isEqualByComparingTo("100");
        assertThat(defaults.marketK()).isEqualByComparingTo("0.05");
        assertThat(defaults.marketMaxChangePct()).isEqualByComparingTo("10");
        assertThat(defaults.marketMinValue()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("The default spades conversion gives -2, 2, 6, 10 and 14 points for 0 to 4 spades")
    void defaultSpadesConversionMatchesRequirements() {
        assertThat(IntStream.rangeClosed(0, 4)
                .mapToObj(spades -> defaults.ratingFactor().multiply(BigDecimal.valueOf(spades))
                        .add(defaults.ratingBase()).intValueExact()))
                .containsExactly(-2, 2, 6, 10, 14);
    }

    @Test
    @DisplayName("The conversion used is the one of the current format, and the other one is kept")
    void conversionUsedIsTheOneOfTheCurrentFormat() {
        LeagueSettings spades = rating(defaults, RatingFormat.SPADES, "3", "-1", "2", "0.5", 1);
        LeagueSettings decimal = rating(spades, RatingFormat.DECIMAL, "3", "-1", "2", "0.5", 1);

        assertThat(spades.ratingFactor()).isEqualByComparingTo("3");
        assertThat(spades.ratingBase()).isEqualByComparingTo("-1");
        assertThat(decimal.ratingFactor()).isEqualByComparingTo("2");
        assertThat(decimal.ratingBase()).isEqualByComparingTo("0.5");
        assertThat(decimal.ratingSpadesFactor()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("Rejects a non-positive conversion factor or a minimum of votes below one")
    void rejectsNonPositiveFactorOrMinimumVotesBelowOne() {
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "0", "-2", "1", "0", 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", "-2", "-1", "0", 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", "-2", "1", "0", 0));
        assertThat(rating(defaults, RatingFormat.SPADES, "4", "-2", "1", "0", 2).ratingMinVotes()).isEqualTo(2);
    }

    @Test
    @DisplayName("A base may be negative, zero or positive but is required")
    void baseMayHaveAnySignButIsRequired() {
        assertThat(rating(defaults, RatingFormat.SPADES, "4", "3", "1", "-5", 1).ratingDecimalBase())
                .isEqualByComparingTo("-5");
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", null, "1", "0", 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", "-2", "1", null, 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, null, "-2", "1", "0", 1));
    }

    @Test
    @DisplayName("Rejects a conversion that does not fit its column: more than 4 integer digits or 2 decimals")
    void rejectsConversionThatDoesNotFitItsColumn() {
        assertThat(rating(defaults, RatingFormat.SPADES, "9999.99", "-9999.99", "0.01", "100", 1)
                .ratingSpadesFactor()).isEqualByComparingTo("9999.99");
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "10000", "-2", "1", "0", 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", "-2", "0.001", "0", 1));
        assertInvalid(s -> rating(s, RatingFormat.SPADES, "4", "-10000", "1", "0", 1));
    }

    @Test
    @DisplayName("Rejects out of range values")
    void rejectsOutOfRangeValues() {
        assertInvalid(s -> with(s, 0, 7, "10"));
        assertInvalid(s -> with(s, 24, 0, "10"));
        assertInvalid(s -> with(s, 24, 7, "0"));
        assertInvalid(s -> with(s, 24, 7, "100.01"));
    }

    @Test
    @DisplayName("The lock hours must not be negative")
    void lockHoursMustNotBeNegative() {
        // the upper bound is configurable: LeagueLimits
        assertThat(lineup(defaults, 169, 7, false, 1, 1, 1, 1).lineupLockHours()).isEqualTo(169);
        assertInvalid(s -> lineup(s, -1, 7, false, 1, 1, 1, 1));
    }

    @Test
    @DisplayName("Rejects negative minimums per position")
    void rejectsNegativeMinimumsPerPosition() {
        assertInvalid(s -> lineup(s, 2, 7, false, -1, 1, 1, 1));
        assertInvalid(s -> lineup(s, 2, 7, false, 1, -1, 1, 1));
        assertInvalid(s -> lineup(s, 2, 7, false, 1, 1, -1, 1));
        assertInvalid(s -> lineup(s, 2, 7, false, 1, 1, 1, -1));
    }

    @Test
    @DisplayName("The minimums cannot exceed the lineup size when the formation is enabled")
    void minimumsCannotExceedLineupSizeWhenFormationIsEnabled() {
        assertThat(lineup(defaults, 2, 7, true, 1, 2, 2, 2).lineupMinDefenders()).isEqualTo(2);
        assertThat(lineup(defaults, 2, 7, true, 0, 0, 0, 0).lineupFormationEnabled()).isTrue();
        assertInvalid(s -> lineup(s, 2, 7, true, 1, 3, 2, 2));
        assertInvalid(s -> lineup(s, 2, 3, true, 1, 1, 1, 1));
    }

    @Test
    @DisplayName("The minimums are kept but not checked against the size when the formation is disabled")
    void minimumsAreKeptButNotCheckedAgainstSizeWhenFormationIsDisabled() {
        LeagueSettings settings = lineup(defaults, 2, 3, false, 1, 1, 1, 1);

        assertThat(settings.lineupSize()).isEqualTo(3);
        assertThat(settings.lineupMinForwards()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects a default rating above the format maximum")
    void rejectsDefaultRatingAboveFormatMaximum() {
        assertInvalid(s -> defaultRating(s, RatingFormat.SPADES, "5"));
        assertInvalid(s -> defaultRating(s, RatingFormat.DECIMAL, "10.01"));
        assertInvalid(s -> defaultRating(s, RatingFormat.DECIMAL, "-0.01"));
    }

    @Test
    @DisplayName("Accepts a default rating within the format maximum")
    void acceptsDefaultRatingWithinFormatMaximum() {
        assertThat(defaultRating(defaults, RatingFormat.DECIMAL, "5").defaultRating()).isEqualByComparingTo("5");
        assertThat(defaultRating(defaults, RatingFormat.SPADES, "4").defaultRating()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("Rejects non-positive money amounts and factors")
    void rejectsNonPositiveMoneyAndFactors() {
        assertInvalid(s -> market(s, "0", "0.05", "1"));
        assertInvalid(s -> market(s, "100", "0", "1"));
        assertInvalid(s -> market(s, "100", "0.05", "0"));
    }

    private static LeagueSettings rating(LeagueSettings s, RatingFormat format, String spadesFactor,
                                         String spadesBase, String decimalFactor, String decimalBase, int minVotes) {
        return new LeagueSettings(s.ratingWindowHours(), format, decimal(spadesFactor), decimal(spadesBase),
                decimal(decimalFactor), decimal(decimalBase), minVotes, s.defaultRating(), s.lineupLockHours(),
                s.lineupMaxChanges(), s.lineupSize(), s.lineupFormationEnabled(), s.lineupMinGoalkeepers(),
                s.lineupMinDefenders(), s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(),
                s.lineupBudget(), s.marketK(), s.marketMaxChangePct(), s.marketMinValue());
    }

    private static LeagueSettings defaultRating(LeagueSettings s, RatingFormat format, String defaultRating) {
        return new LeagueSettings(s.ratingWindowHours(), format, s.ratingSpadesFactor(), s.ratingSpadesBase(),
                s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(), new BigDecimal(defaultRating),
                s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(), s.lineupFormationEnabled(),
                s.lineupMinGoalkeepers(), s.lineupMinDefenders(), s.lineupMinMidfielders(), s.lineupMinForwards(),
                s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(), s.marketMaxChangePct(), s.marketMinValue());
    }

    private static LeagueSettings market(LeagueSettings s, String budget, String k, String minValue) {
        return new LeagueSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingSpadesFactor(),
                s.ratingSpadesBase(), s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(),
                s.defaultRating(), s.lineupLockHours(), s.lineupMaxChanges(), s.lineupSize(),
                s.lineupFormationEnabled(), s.lineupMinGoalkeepers(), s.lineupMinDefenders(),
                s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(), new BigDecimal(budget),
                new BigDecimal(k), s.marketMaxChangePct(), new BigDecimal(minValue));
    }

    private static LeagueSettings with(LeagueSettings s, int windowHours, int size, String maxChangePct) {
        return new LeagueSettings(windowHours, s.ratingFormat(), s.ratingSpadesFactor(), s.ratingSpadesBase(),
                s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(), s.defaultRating(),
                s.lineupLockHours(), s.lineupMaxChanges(), size, s.lineupFormationEnabled(), s.lineupMinGoalkeepers(),
                s.lineupMinDefenders(), s.lineupMinMidfielders(), s.lineupMinForwards(), s.lineupBudgetEnabled(),
                s.lineupBudget(), s.marketK(), new BigDecimal(maxChangePct), s.marketMinValue());
    }

    private static LeagueSettings lineup(LeagueSettings s, int lockHours, int size, boolean formation,
                                         int goalkeepers, int defenders, int midfielders, int forwards) {
        return new LeagueSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingSpadesFactor(),
                s.ratingSpadesBase(), s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(),
                s.defaultRating(), lockHours, s.lineupMaxChanges(), size, formation, goalkeepers, defenders,
                midfielders, forwards, s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(),
                s.marketMaxChangePct(), s.marketMinValue());
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private void assertInvalid(UnaryOperator<LeagueSettings> build) {
        assertThatThrownBy(() -> build.apply(defaults)).isInstanceOf(IllegalArgumentException.class);
    }
}
