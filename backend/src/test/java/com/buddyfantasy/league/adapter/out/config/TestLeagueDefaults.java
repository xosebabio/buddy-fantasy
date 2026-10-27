package com.buddyfantasy.league.adapter.out.config;

import java.util.Map;

import com.buddyfantasy.TestApplicationYaml;
import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import lombok.experimental.UtilityClass;

/** {@code league.defaults} from the real {@code application.yml}, for unit tests. */
@UtilityClass
public class TestLeagueDefaults {

    private final LeagueDefaultsProperties PROPERTIES = bind(Map.of());

    public LeagueSettings settings() {
        return LeagueDefaultsMapper.toSettings(PROPERTIES);
    }

    public ScoringRules scoringRules() {
        return LeagueDefaultsMapper.toScoringRules(PROPERTIES);
    }

    /** Binds {@code application.yml} with {@code overrides} (full property names) taking precedence. */
    LeagueDefaultsProperties bind(Map<String, Object> overrides) {
        return TestApplicationYaml.bind("league.defaults", LeagueDefaultsProperties.class, overrides);
    }
}
