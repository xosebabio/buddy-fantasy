package com.buddyfantasy.league.domain;

import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import com.buddyfantasy.league.adapter.out.config.TestLeagueLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueLimitsTests {

    private final LeagueLimits limits = TestLeagueLimits.get();

    @Test
    @DisplayName("The configured limits match the requirements")
    void configuredLimitsMatchRequirements() {
        assertThat(limits.nameMaxLength()).isEqualTo(100);
        assertThat(limits.lineupLockHoursMax()).isEqualTo(168);
    }

    @Test
    @DisplayName("The name must be non-blank and within the maximum length")
    void nameMustBeNonBlankAndWithinTheMaximumLength() {
        assertThatCode(() -> limits.requireValidName("x".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValidName("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidName("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidName(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("The lock hours must not exceed the maximum")
    void lockHoursMustNotExceedTheMaximum() {
        LeagueSettings atMax = withLockHours(168);
        LeagueSettings overMax = withLockHours(169);

        assertThatCode(() -> limits.requireWithin(atMax)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireWithin(overMax)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new LeagueLimits(100, 200).requireWithin(overMax)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create and withSettings check the limits")
    void createAndWithSettingsCheckTheLimits() {
        LeagueSettings settings = TestLeagueDefaults.settings();
        ScoringRules rules = TestLeagueDefaults.scoringRules();

        assertThatThrownBy(() -> League.create("x".repeat(101), null, settings, rules, limits))
                .isInstanceOf(IllegalArgumentException.class);
        League league = League.create("Peña", null, settings, rules, limits);
        assertThatThrownBy(() -> league.withSettings(withLockHours(169), rules, limits))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects nonsense limits")
    void rejectsNonsenseLimits() {
        assertThatThrownBy(() -> new LeagueLimits(0, 168)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueLimits(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static LeagueSettings withLockHours(int lockHours) {
        LeagueSettings s = TestLeagueDefaults.settings();
        return new LeagueSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingSpadesFactor(),
                s.ratingSpadesBase(), s.ratingDecimalFactor(), s.ratingDecimalBase(), s.ratingMinVotes(),
                s.defaultRating(), lockHours, s.lineupMaxChanges(), s.lineupSize(), s.lineupFormationEnabled(),
                s.lineupMinGoalkeepers(), s.lineupMinDefenders(), s.lineupMinMidfielders(), s.lineupMinForwards(),
                s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(), s.marketMaxChangePct(), s.marketMinValue());
    }
}
