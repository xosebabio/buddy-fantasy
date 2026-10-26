package com.buddyfantasy.squad.application.usecases.listplayers;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;

/** The league's squad. {@code includeRetired} is honoured only for the league admin and ignored otherwise. */
public record ListPlayersQuery(UUID userId, UUID leagueId, boolean includeRetired) implements Query<List<PlayerView>> {
}
