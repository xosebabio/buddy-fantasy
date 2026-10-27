package com.buddyfantasy.league.application.port.out;

import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;

/** Configured values every new league starts with (RF-07). Never hard-coded: they come from configuration. */
public interface LeagueDefaultsRepository {

    LeagueSettings settings();

    ScoringRules scoringRules();
}
