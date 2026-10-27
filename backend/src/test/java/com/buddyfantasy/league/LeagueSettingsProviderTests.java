package com.buddyfantasy.league;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.shared.domain.NotFoundException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void reset() {
        jdbc.update("delete from league_membership");
        jdbc.update("delete from league_invitation");
        jdbc.update("delete from league_scoring_rule");
        jdbc.update("delete from league_settings");
        jdbc.update("delete from league_league");
    }

    private League league(String name) {
        League league = League.create(name, NOW);
        leagues.save(league);
        return league;
    }

    private League leagueWithFormation() {
        League league = league("Formation");
        LeagueSettings d = LeagueSettings.defaults();
        LeagueSettings settings = new LeagueSettings(d.ratingWindowHours(), d.ratingFormat(),
                d.ratingPointsPerUnit(), d.defaultRating(), 24, 3, 9, true, 1, 3, 2, 0, false,
                new BigDecimal("80"), d.marketK(), d.marketMaxChangePct(), d.marketMinValue());
        League updated = league.withSettings(settings, league.scoringRules());
        leagues.save(updated);
        return updated;
    }

    @Test
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
    void unknownLeagueIsNotFound() {
        assertThatThrownBy(() -> provider.lineupSettings(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
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

    @Test
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
