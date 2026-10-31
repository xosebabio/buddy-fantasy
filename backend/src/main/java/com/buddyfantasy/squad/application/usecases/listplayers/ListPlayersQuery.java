package com.buddyfantasy.squad.application.usecases.listplayers;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;

/**
 * The league's squad, for any member. The retired players are not secret (they are in the sheets, the points and
 * {@code GET /players/{id}}): {@code includeRetired} adds them, so the screens can resolve every id they show.
 */
public record ListPlayersQuery(UUID userId, UUID leagueId, boolean includeRetired) implements Query<List<PlayerView>> {
}
