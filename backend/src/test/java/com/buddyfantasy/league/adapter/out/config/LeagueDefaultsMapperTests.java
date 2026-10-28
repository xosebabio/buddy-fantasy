package com.buddyfantasy.league.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.datadefinition.ScoringAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Binding of {@code league.defaults}: overridable per property and validated by the domain. */
class LeagueDefaultsMapperTests {

    @Test
    @DisplayName("Binds every group of the configured defaults")
    void bindsEveryGroupOfTheConfiguredDefaults() {
        LeagueDefaultsProperties properties = TestLeagueDefaults.bind(Map.of());

        assertThat(properties.rating().format()).isEqualTo(RatingFormat.SPADES);
        assertThat(properties.rating().defaultRating()).isNull();
        assertThat(properties.rating().conversion().spades().factor()).isEqualByComparingTo("4");
        assertThat(properties.rating().conversion().spades().base()).isEqualByComparingTo("-2");
        assertThat(properties.rating().conversion().decimal().factor()).isEqualByComparingTo("1");
        assertThat(properties.rating().conversion().decimal().base()).isEqualByComparingTo("0");
        assertThat(properties.rating().minVotes()).isEqualTo(1);
        assertThat(properties.lineup().size()).isEqualTo(7);
        assertThat(properties.market().k()).isEqualByComparingTo("0.05");
        assertThat(properties.scoringRules()).hasSize(ScoringAction.values().length);
        assertThat(properties.scoringRules().get(ScoringAction.PENALTY_GOAL).get(Position.FORWARD)).isEqualTo(3);
    }

    @Test
    @DisplayName("Any default can be overridden")
    void anyDefaultCanBeOverridden() {
        LeagueDefaultsProperties properties = TestLeagueDefaults.bind(Map.of(
                "league.defaults.lineup.size", "9",
                "league.defaults.rating.format", "DECIMAL",
                "league.defaults.rating.default-rating", "5",
                "league.defaults.rating.conversion.decimal.factor", "1.5",
                "league.defaults.rating.min-votes", "2",
                "league.defaults.scoring-rules.goal.forward", "4"));

        assertThat(LeagueDefaultsMapper.toSettings(properties).lineupSize()).isEqualTo(9);
        assertThat(LeagueDefaultsMapper.toSettings(properties).defaultRating()).isEqualByComparingTo("5");
        assertThat(LeagueDefaultsMapper.toSettings(properties).ratingFactor()).isEqualByComparingTo("1.5");
        assertThat(LeagueDefaultsMapper.toSettings(properties).ratingBase()).isEqualByComparingTo("0");
        assertThat(LeagueDefaultsMapper.toSettings(properties).ratingSpadesFactor()).isEqualByComparingTo("4");
        assertThat(LeagueDefaultsMapper.toSettings(properties).ratingMinVotes()).isEqualTo(2);
        assertThat(LeagueDefaultsMapper.toScoringRules(properties).pointsFor(ScoringAction.GOAL, Position.FORWARD))
                .isEqualTo(4);
        assertThat(LeagueDefaultsMapper.toScoringRules(properties).pointsFor(ScoringAction.GOAL, Position.GOALKEEPER))
                .isEqualTo(6);
    }

    @Test
    @DisplayName("Invalid defaults are rejected")
    void invalidDefaultsAreRejected() {
        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                TestLeagueDefaults.bind(Map.of("league.defaults.lineup.size", "0"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                TestLeagueDefaults.bind(Map.of("league.defaults.rating.conversion.spades.factor", "0"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ratingSpadesFactor");
        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                TestLeagueDefaults.bind(Map.of("league.defaults.rating.min-votes", "0"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ratingMinVotes");
    }

    @Test
    @DisplayName("Defaults over the limits stop the repository from being created")
    void defaultsOverTheLimitsStopTheRepositoryFromBeingCreated() {
        LeagueDefaultsProperties overLimit = TestLeagueDefaults.bind(
                Map.of("league.defaults.lineup.lock-hours", "169"));

        assertThatThrownBy(() -> new PropertiesLeagueDefaultsRepository(overLimit, TestLeagueLimits::get))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lineupLockHours");
        assertThat(new PropertiesLeagueDefaultsRepository(TestLeagueDefaults.bind(Map.of()), TestLeagueLimits::get)
                .settings().lineupLockHours()).isEqualTo(2);
    }

    @Test
    @DisplayName("Missing groups or an incomplete scoring table are rejected")
    void missingGroupsOrIncompleteScoringTableAreRejected() {
        LeagueDefaultsProperties full = TestLeagueDefaults.bind(Map.of());

        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(
                new LeagueDefaultsProperties(full.rating(), null, full.market(), full.scoringRules())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("league.defaults.lineup");
        LeagueDefaultsProperties.Rating rating = full.rating();
        assertThatThrownBy(() -> LeagueDefaultsMapper.toSettings(new LeagueDefaultsProperties(
                new LeagueDefaultsProperties.Rating(rating.windowHours(), rating.format(),
                        new LeagueDefaultsProperties.Conversions(rating.conversion().spades(), null),
                        rating.minVotes(), rating.defaultRating()),
                full.lineup(), full.market(), full.scoringRules())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("league.defaults.rating.conversion.decimal");
        assertThatThrownBy(() -> LeagueDefaultsMapper.toScoringRules(
                new LeagueDefaultsProperties(full.rating(), full.lineup(), full.market(),
                        Map.of(ScoringAction.GOAL, full.scoringRules().get(ScoringAction.GOAL)))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
