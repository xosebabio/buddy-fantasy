package com.buddyfantasy.squad.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.TestApplicationYaml;
import com.buddyfantasy.squad.domain.SquadLimits;
import lombok.experimental.UtilityClass;

/** {@code squad.limits} from the real {@code application.yml}, for unit tests. */
@UtilityClass
public class TestSquadLimits {

    private final SquadLimits LIMITS = new PropertiesSquadLimitsRepository(
            TestApplicationYaml.bind("squad.limits", SquadLimitsProperties.class, Map.of())).get();

    public SquadLimits get() {
        return LIMITS;
    }
}
