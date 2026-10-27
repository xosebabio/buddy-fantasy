package com.buddyfantasy.league.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.TestApplicationYaml;
import com.buddyfantasy.league.domain.LeagueLimits;
import lombok.experimental.UtilityClass;

/** {@code league.limits} from the real {@code application.yml}, for unit tests. */
@UtilityClass
public class TestLeagueLimits {

    private final LeagueLimits LIMITS = new PropertiesLeagueLimitsRepository(
            TestApplicationYaml.bind("league.limits", LeagueLimitsProperties.class, Map.of())).get();

    public LeagueLimits get() {
        return LIMITS;
    }
}
