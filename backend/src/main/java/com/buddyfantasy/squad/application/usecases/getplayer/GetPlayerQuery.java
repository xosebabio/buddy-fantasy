package com.buddyfantasy.squad.application.usecases.getplayer;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/** RF-11: any league member sees a player's card, retired players included. */
public record GetPlayerQuery(UUID userId, UUID leagueId, UUID playerId) implements Query<PlayerView> {
}
