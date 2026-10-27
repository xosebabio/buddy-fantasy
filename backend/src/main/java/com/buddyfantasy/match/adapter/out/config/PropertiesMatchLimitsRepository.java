package com.buddyfantasy.match.adapter.out.config;

import com.buddyfantasy.match.application.port.out.MatchLimitsRepository;
import com.buddyfantasy.match.domain.MatchLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Repository;

/** Built once at startup from {@code match.limits}: invalid configuration fails fast. */
@Repository
@EnableConfigurationProperties(MatchLimitsProperties.class)
class PropertiesMatchLimitsRepository implements MatchLimitsRepository {

    private final MatchLimits limits;

    PropertiesMatchLimitsRepository(MatchLimitsProperties properties) {
        this.limits = new MatchLimits(properties.opponentMaxLength(), properties.matchdayMin(),
                properties.goalsMax(), properties.statCountMax(), properties.sheetPlayersMax());
    }

    @Override
    public MatchLimits get() {
        return limits;
    }
}
