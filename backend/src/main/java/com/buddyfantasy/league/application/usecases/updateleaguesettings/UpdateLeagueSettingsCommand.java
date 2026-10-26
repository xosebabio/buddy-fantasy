package com.buddyfantasy.league.application.usecases.updateleaguesettings;

import java.util.UUID;

import com.buddyfantasy.league.domain.LeagueSettings;
import com.buddyfantasy.league.domain.ScoringRules;
import com.buddyfantasy.shared.application.Command;

/** RF-07: full replacement of a league's parameters. Does not recalculate past matchdays. */
public record UpdateLeagueSettingsCommand(UUID userId, UUID leagueId, LeagueSettings settings, ScoringRules scoringRules)
        implements Command<Void> {
}
