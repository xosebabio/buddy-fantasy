package com.buddyfantasy.league.application.usecases.getleague;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetLeagueQuery(UUID userId, UUID leagueId) implements Query<LeagueView> {
}
