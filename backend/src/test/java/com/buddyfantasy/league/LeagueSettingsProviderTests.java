package com.buddyfantasy.league;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import com.buddyfantasy.shared.domain.NotFoundException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
@Import(PostgresTestConfig.class)
class LeagueSettingsProviderTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    LeagueSettingsProvider provider;

    @Autowired
    LeagueRepository leagues;

    @Autowired
    LeagueDefaultsRepository defaults;

    @Autowired
    LeagueLimitsRepository limits;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private League league(String name) {
        League league = League.create(name, NOW, defaults.settings(), defaults.scoringRules(), limits.get());
        leagues.save(league);
        return league;
    }

    private League leagueWithFormation() {
        League league = league("Formation");
        LeagueSettings d = defaults.settings();
        LeagueSettings settings = new LeagueSettings(d.ratingWindowHours(), d.ratingFormat(),
                d.ratingPointsPerUnit(), d.defaultRating(), 24, 3, 9, true, 1, 3, 2, 0, false,
                new BigDecimal("80"), d.marketK(), d.marketMaxChangePct(), d.marketMinValue());
        League updated = league.withSettings(settings, league.scoringRules(), limits.get());
        leagues.save(updated);
        return updated;
    }

    @Test
    @DisplayName("Returns the lineup parameters of the league")
    void returnsTheLineupParametersOfTheLeague() {
        League league = leagueWithFormation();

        LineupSettings settings = provider.lineupSettings(league.id().value());

        assertThat(settings.size()).isEqualTo(9);
        assertThat(settings.lockHours()).isEqualTo(24);
        assertThat(settings.maxChanges()).isEqualTo(3);
        assertThat(settings.formationEnabled()).isTrue();
        assertThat(settings.minGoalkeepers()).isEqualTo(1);
        assertThat(settings.minDefenders()).isEqualTo(3);
        assertThat(settings.minMidfielders()).isEqualTo(2);
        assertThat(settings.minForwards()).isZero();
        assertThat(settings.budgetEnabled()).isFalse();
        assertThat(settings.budget()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("A default league has the default lineup parameters")
    void defaultLeagueHasTheDefaultLineupParameters() {
        League league = league("Default");

        LineupSettings settings = provider.lineupSettings(league.id().value());

        assertThat(settings.size()).isEqualTo(7);
        assertThat(settings.lockHours()).isEqualTo(2);
        assertThat(settings.maxChanges()).isEqualTo(2);
        assertThat(settings.formationEnabled()).isFalse();
        assertThat(List.of(settings.minGoalkeepers(), settings.minDefenders(), settings.minMidfielders(),
                settings.minForwards())).containsOnly(1);
        assertThat(settings.budgetEnabled()).isTrue();
        assertThat(settings.budget()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("An unknown league is not found")
    void unknownLeagueIsNotFound() {
        assertThatThrownBy(() -> provider.lineupSettings(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("The batch returns only the requested leagues in a single query")
    void batchReturnsOnlyTheRequestedLeaguesInASingleQuery() {
        League a = leagueWithFormation();
        League b = league("B");
        league("Not asked");
        UUID unknown = UUID.randomUUID();
        Statistics statistics = statistics();

        Map<UUID, LineupSettings> result = provider.lineupSettings(List.of(a.id().value(), b.id().value(), unknown));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(result).containsOnlyKeys(a.id().value(), b.id().value());
        assertThat(result.get(a.id().value()).size()).isEqualTo(9);
        assertThat(result.get(b.id().value()).size()).isEqualTo(7);
    }

    private League leagueWithEditedScoringRules() {
        League league = league("Edited rules");
        Map<ScoringAction, Map<Position, Integer>> points = new EnumMap<>(league.scoringRules().points());
        Map<Position, Integer> goal = new EnumMap<>(points.get(ScoringAction.GOAL));
        goal.put(Position.FORWARD, 8);
        points.put(ScoringAction.GOAL, goal);
        League updated = league.withSettings(league.settings(), new ScoringRules(points), limits.get());
        leagues.save(updated);
        return updated;
    }

    @Test
    @DisplayName("Returns the scoring table of the requested league in a single query")
    void returnsTheScoringTableOfTheRequestedLeagueInASingleQuery() {
        League edited = leagueWithEditedScoringRules();
        league("Other");
        Statistics statistics = statistics();

        ScoringTable table = provider.scoringRules(edited.id().value());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(table.pointsFor(ScoringAction.GOAL, Position.FORWARD)).isEqualTo(8);
        assertThat(table.pointsFor(ScoringAction.GOAL, Position.GOALKEEPER))
                .isEqualTo(defaults.scoringRules().pointsFor(ScoringAction.GOAL, Position.GOALKEEPER));
        assertThat(table.points()).isEqualTo(edited.scoringRules().points());
    }

    @Test
    @DisplayName("A default league has the default scoring table")
    void defaultLeagueHasTheDefaultScoringTable() {
        League league = league("Default");

        ScoringTable table = provider.scoringRules(league.id().value());

        assertThat(table.points()).isEqualTo(defaults.scoringRules().points());
        assertThat(table.pointsFor(ScoringAction.GOAL, Position.GOALKEEPER)).isEqualTo(6);
        assertThat(table.pointsFor(ScoringAction.PENALTY_GOAL, Position.FORWARD)).isEqualTo(3);
        assertThat(table.pointsFor(ScoringAction.DOUBLE_YELLOW, Position.DEFENDER)).isEqualTo(-3);
    }

    @Test
    @DisplayName("The scoring table of an unknown league is not found")
    void scoringTableOfAnUnknownLeagueIsNotFound() {
        assertThatThrownBy(() -> provider.scoringRules(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Exposes the configured lock hours maximum")
    void exposesTheConfiguredLockHoursMaximum() {
        assertThat(provider.lineupLockHoursMax()).isEqualTo(limits.get().lineupLockHoursMax());
    }

    @Test
    @DisplayName("An empty batch returns nothing without querying")
    void emptyBatchReturnsNothingWithoutQuerying() {
        Statistics statistics = statistics();

        assertThat(provider.lineupSettings(List.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    private Statistics statistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
