package com.buddyfantasy.league.application.usecases.getleaguelimits;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetLeagueLimitsQuery(UUID userId, UUID leagueId) implements Query<LeagueLimitsView> {
}
