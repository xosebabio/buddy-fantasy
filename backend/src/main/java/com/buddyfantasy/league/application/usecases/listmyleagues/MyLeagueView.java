package com.buddyfantasy.league.application.usecases.listmyleagues;

import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;

public record MyLeagueView(UUID leagueId, String name, LeagueRole role) {
}
