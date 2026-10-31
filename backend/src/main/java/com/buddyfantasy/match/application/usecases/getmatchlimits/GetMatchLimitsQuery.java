package com.buddyfantasy.match.application.usecases.getmatchlimits;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetMatchLimitsQuery(UUID userId, UUID leagueId) implements Query<MatchLimitsView> {
}
