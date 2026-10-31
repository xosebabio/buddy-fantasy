package com.buddyfantasy.squad.application.usecases.getsquadlimits;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetSquadLimitsQuery(UUID userId, UUID leagueId) implements Query<SquadLimitsView> {
}
