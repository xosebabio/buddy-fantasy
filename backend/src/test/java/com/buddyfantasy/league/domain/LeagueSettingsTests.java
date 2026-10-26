package com.buddyfantasy.league.domain;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueSettingsTests {

    private final LeagueSettings defaults = LeagueSettings.defaults();

    @Test
    void defaultsMatchRequirements() {
        assertThat(defaults.ratingWindowHours()).isEqualTo(24);
        assertThat(defaults.ratingFormat()).isEqualTo(RatingFormat.SPADES);
        assertThat(defaults.ratingPointsPerUnit()).isEqualByComparingTo("1");
        assertThat(defaults.defaultRating()).isNull();
        assertThat(defaults.lineupLockHours()).isEqualTo(2);
        assertThat(defaults.lineupMaxChanges()).isEqualTo(2);
        assertThat(defaults.lineupSize()).isEqualTo(7);
        assertThat(defaults.lineupFormationEnabled()).isFalse();
        assertThat(defaults.lineupBudgetEnabled()).isTrue();
        assertThat(defaults.lineupBudget()).isEqualByComparingTo("100");
        assertThat(defaults.marketK()).isEqualByComparingTo("0.05");
        assertThat(defaults.marketMaxChangePct()).isEqualByComparingTo("10");
        assertThat(defaults.marketMinValue()).isEqualByComparingTo("1");
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertInvalid(s -> with(s, 0, 7, "10"));
        assertInvalid(s -> with(s, 24, 0, "10"));
        assertInvalid(s -> with(s, 24, 7, "0"));
        assertInvalid(s -> with(s, 24, 7, "100.01"));
    }

    @Test
    void rejectsDefaultRatingAboveFormatMaximum() {
        assertThatThrownBy(() -> new LeagueSettings(24, RatingFormat.SPADES, BigDecimal.ONE, new BigDecimal("5"),
                2, 2, 7, false, false, new BigDecimal("100"), new BigDecimal("0.05"), BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsDefaultRatingWithinFormatMaximum() {
        LeagueSettings settings = new LeagueSettings(24, RatingFormat.DECIMAL, BigDecimal.ONE, new BigDecimal("5"),
                2, 2, 7, false, false, new BigDecimal("100"), new BigDecimal("0.05"), BigDecimal.TEN, BigDecimal.ONE);
        assertThat(settings.defaultRating()).isEqualByComparingTo("5");
    }

    @Test
    void rejectsNonPositiveMoneyAndFactors() {
        assertThatThrownBy(() -> new LeagueSettings(24, RatingFormat.SPADES, BigDecimal.ONE, null,
                2, 2, 7, false, false, BigDecimal.ZERO, new BigDecimal("0.05"), BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueSettings(24, RatingFormat.SPADES, BigDecimal.ONE, null,
                2, 2, 7, false, false, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueSettings(24, RatingFormat.SPADES, BigDecimal.ONE, null,
                2, 2, 7, false, false, new BigDecimal("100"), new BigDecimal("0.05"), BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static LeagueSettings with(LeagueSettings s, int windowHours, int size, String maxChangePct) {
        return new LeagueSettings(windowHours, s.ratingFormat(), s.ratingPointsPerUnit(), s.defaultRating(),
                s.lineupLockHours(), s.lineupMaxChanges(), size, s.lineupFormationEnabled(), s.lineupBudgetEnabled(),
                s.lineupBudget(), s.marketK(), new BigDecimal(maxChangePct), s.marketMinValue());
    }

    private void assertInvalid(UnaryOperator<LeagueSettings> build) {
        assertThatThrownBy(() -> build.apply(defaults)).isInstanceOf(IllegalArgumentException.class);
    }
}
