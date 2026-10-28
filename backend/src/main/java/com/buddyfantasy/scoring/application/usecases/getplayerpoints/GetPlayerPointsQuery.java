package com.buddyfantasy.scoring.application.usecases.getplayerpoints;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * The points of a player of the league (RF-11) in every scored match in which they were in the sheet, retired or not.
 * 404 if the player is not of the league.
 */
public record GetPlayerPointsQuery(UUID userId, UUID leagueId, UUID playerId)
        implements Query<List<PlayerMatchdayView>> {
}
