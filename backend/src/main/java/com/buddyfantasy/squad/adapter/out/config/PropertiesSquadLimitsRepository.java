package com.buddyfantasy.squad.adapter.out.config;

import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.domain.SquadLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Repository;

/** Built once at startup from {@code squad.limits}: invalid configuration fails fast. */
@Repository
@EnableConfigurationProperties(SquadLimitsProperties.class)
class PropertiesSquadLimitsRepository implements SquadLimitsRepository {

    private final SquadLimits limits;

    PropertiesSquadLimitsRepository(SquadLimitsProperties properties) {
        long photoMaxSizeBytes = properties.photoMaxSize() == null ? 0 : properties.photoMaxSize().toBytes();
        this.limits = new SquadLimits(properties.nameMaxLength(), properties.shirtNumberMin(),
                properties.shirtNumberMax(), photoMaxSizeBytes);
    }

    @Override
    public SquadLimits get() {
        return limits;
    }
}
