package com.buddyfantasy.league.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.domain.RatingFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Binding of {@code league.defaults}: overridable per property and validated by the domain. */
class LeagueDefaultsMapperTests {

    @Test
    void bindsEveryGroupOfTheConfiguredDefaults() {
        LeagueDefaultsProperties properties = TestLeagueDefaults.bind(Map.of());

        assertThat(properties.rating().format()).isEqualTo(RatingFormat.SPADES);
        assertThat(properties.rating().defaultRating()).isNull();
        assertThat(properties.lineup().size()).isEqualTo(7);
        assertThat(properties.market().k()).isEqualByComparingTo("0.05");
        assertThat(properties.scoringRules()).hasSize(ScoringAction.values().length);
        assertThat(properties.scoringRules().get(ScoringAction.PENALTY_GOAL).get(Position.FORWARD)).isEqualTo(3);
    }

    @Test
    void anyDefaultCanBeOverridden() {
        LeagueDefaultsProperties properties = TestLeagueDefaults.bind(Map.of(
                "league.defaults.lineup.size", "9",
                "league.defaults.rating.format", "DECIMAL",
                "league.defaults.rating.default-rating", "5",
                "league.defaults.scoring-rules.goal.forward", "4"));

        assertThat(LeagueDefaultsMapper.toSettings(properties).lineupSize()).isEqualTo(9);
        assertThat(LeagueDefaultsMapper.toSettings(properties).defaultRating()).isEqualByComparingTo("5");
        assertThat(LeagueDefaultsMapper.toScoringRules(properties).pointsFor(ScoringAction.GOAL, Position.FORWARD))
                .isEqualTo(4);
        assertThat(LeagueDefaultsMapper.toScoringRules(properties).pointsFor(ScoringAction.GOAL, Position.GOALKEEPER))
                .isEqualTo(6);
    }

    @Test
    void invalidDefaultsAreRejected() {
        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                TestLeagueDefaults.bind(Map.of("league.defaults.lineup.size", "0"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsOverTheLimitsStopTheRepositoryFromBeingCreated() {
        LeagueDefaultsProperties overLimit = TestLeagueDefaults.bind(
                Map.of("league.defaults.lineup.lock-hours", "169"));

        assertThatThrownBy(() -> new PropertiesLeagueDefaultsRepository(overLimit, TestLeagueLimits::get))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lineupLockHours");
        assertThat(new PropertiesLeagueDefaultsRepository(TestLeagueDefaults.bind(Map.of()), TestLeagueLimits::get)
                .settings().lineupLockHours()).isEqualTo(2);
    }

    @Test
    void missingGroupsOrIncompleteScoringTableAreRejected() {
        LeagueDefaultsProperties full = TestLeagueDefaults.bind(Map.of());

        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                new LeagueDefaultsProperties(full.rating(), null, full.market(), full.scoringRules())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("league.defaults.lineup");
        assertThatThrownBy(() -> LeagueDefaultsMapper.toScoringRules(
                new LeagueDefaultsProperties(full.rating(), full.lineup(), full.market(),
                        Map.of(ScoringAction.GOAL, full.scoringRules().get(ScoringAction.GOAL)))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
