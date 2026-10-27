package com.buddyfantasy.league.adapter.out.config;

import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.domain.LeagueLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Repository;

/** Built once at startup from {@code league.limits}: invalid configuration fails fast. */
@Repository
@EnableConfigurationProperties(LeagueLimitsProperties.class)
class PropertiesLeagueLimitsRepository implements LeagueLimitsRepository {

    private final LeagueLimits limits;

    PropertiesLeagueLimitsRepository(LeagueLimitsProperties properties) {
        this.limits = new LeagueLimits(properties.nameMaxLength(), properties.lineupLockHoursMax());
    }

    @Override
    public LeagueLimits get() {
        return limits;
    }
}
