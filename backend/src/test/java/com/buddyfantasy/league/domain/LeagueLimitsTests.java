package com.buddyfantasy.league.domain;

import com.buddyfantasy.league.adapter.out.config.TestLeagueDefaults;
import com.buddyfantasy.league.adapter.out.config.TestLeagueLimits;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueLimitsTests {

    private final LeagueLimits limits = TestLeagueLimits.get();

    @Test
    void configuredLimitsMatchRequirements() {
        assertThat(limits.nameMaxLength()).isEqualTo(100);
        assertThat(limits.lineupLockHoursMax()).isEqualTo(168);
    }

    @Test
    void nameMustBeNonBlankAndWithinTheMaximumLength() {
        assertThatCode(() -> limits.requireValidName("x".repeat(100))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireValidName("x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidName("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limits.requireValidName(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lockHoursMustNotExceedTheMaximum() {
        LeagueSettings atMax = withLockHours(168);
        LeagueSettings overMax = withLockHours(169);

        assertThatCode(() -> limits.requireWithin(atMax)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limits.requireWithin(overMax)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new LeagueLimits(100, 200).requireWithin(overMax)).doesNotThrowAnyException();
    }

    @Test
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
    void rejectsNonsenseLimits() {
        assertThatThrownBy(() -> new LeagueLimits(0, 168)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LeagueLimits(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static LeagueSettings withLockHours(int lockHours) {
        LeagueSettings s = TestLeagueDefaults.settings();
        return new LeagueSettings(s.ratingWindowHours(), s.ratingFormat(), s.ratingPointsPerUnit(),
                s.defaultRating(), lockHours, s.lineupMaxChanges(), s.lineupSize(), s.lineupFormationEnabled(),
                s.lineupMinGoalkeepers(), s.lineupMinDefenders(), s.lineupMinMidfielders(), s.lineupMinForwards(),
                s.lineupBudgetEnabled(), s.lineupBudget(), s.marketK(), s.marketMaxChangePct(), s.marketMinValue());
    }
}
