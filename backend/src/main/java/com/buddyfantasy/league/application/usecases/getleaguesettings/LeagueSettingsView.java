package com.buddyfantasy.league.application.usecases.getleaguesettings;

import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;

public record LeagueSettingsView(LeagueSettings settings, ScoringRules scoringRules) {
}
