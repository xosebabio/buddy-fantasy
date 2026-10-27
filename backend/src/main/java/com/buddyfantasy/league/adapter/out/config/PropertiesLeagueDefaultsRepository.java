package com.buddyfantasy.league.adapter.out.config;

import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Repository;

/** Built once at startup from {@code league.defaults}: invalid values, or values over the limits, fail fast. */
@Repository
@EnableConfigurationProperties(LeagueDefaultsProperties.class)
class PropertiesLeagueDefaultsRepository implements LeagueDefaultsRepository {

    private final LeagueSettings settings;
    private final ScoringRules scoringRules;

    PropertiesLeagueDefaultsRepository(LeagueDefaultsProperties properties, LeagueLimitsRepository limits) {
        this.settings = LeagueDefaultsMapper.toSettings(properties);
        this.scoringRules = LeagueDefaultsMapper.toScoringRules(properties);
        limits.get().requireWithin(settings);
    }

    @Override
    public LeagueSettings settings() {
        return settings;
    }

    @Override
    public ScoringRules scoringRules() {
        return scoringRules;
    }
}
