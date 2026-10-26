package com.buddyfantasy.league.application.usecases.getleaguesettings;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetLeagueSettingsQuery(UUID userId, UUID leagueId) implements Query<LeagueSettingsView> {
}
