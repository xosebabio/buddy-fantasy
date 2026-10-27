package com.buddyfantasy.lineup.adapter.out.squad;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.lineup.application.port.out.PlayerCatalog;
import com.buddyfantasy.lineup.domain.LineupPlayer;
import com.buddyfantasy.squad.SquadApi;
import com.buddyfantasy.squad.SquadPlayer;
import org.springframework.stereotype.Component;

/**
 * Players from {@code squad}. Until {@code market} exists the current value is the initial value, which the admin
 * may edit; {@code market} will only change this adapter.
 */
@Component
class SquadPlayerCatalog implements PlayerCatalog {

    private final SquadApi squad;

    SquadPlayerCatalog(SquadApi squad) {
        this.squad = squad;
    }

    @Override
    public Map<UUID, LineupPlayer> findPlayers(UUID leagueId, Collection<UUID> playerIds) {
        return squad.findPlayers(leagueId, playerIds).stream()
                .collect(Collectors.toMap(SquadPlayer::id, SquadPlayerCatalog::toLineupPlayer));
    }

    private static LineupPlayer toLineupPlayer(SquadPlayer player) {
        return new LineupPlayer(player.id(), player.position(), player.active(), player.initialValue());
    }
}
