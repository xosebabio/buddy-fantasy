package com.buddyfantasy.match.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.TestApplicationYaml;
import com.buddyfantasy.match.domain.MatchLimits;
import lombok.experimental.UtilityClass;

/** {@code match.limits} from the real {@code application.yml}, for unit tests. */
@UtilityClass
public class TestMatchLimits {

    private final MatchLimits LIMITS = new PropertiesMatchLimitsRepository(
            TestApplicationYaml.bind("match.limits", MatchLimitsProperties.class, Map.of())).get();

    public MatchLimits get() {
        return LIMITS;
    }
}
