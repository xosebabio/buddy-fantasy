package com.buddyfantasy.squad.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.squad.SquadApi;
import com.buddyfantasy.squad.SquadPlayer;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import org.springframework.stereotype.Service;

@Service
class SquadApiService implements SquadApi {

    private final PlayerRepository players;

    SquadApiService(PlayerRepository players) {
        this.players = players;
    }

    @Override
    public List<SquadPlayer> findPlayers(UUID leagueId, Collection<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return List.of();
        }
        List<PlayerId> ids = playerIds.stream().distinct().map(PlayerId::new).toList();
        return players.findByLeagueIdAndIds(leagueId, ids).stream()
                .map(SquadApiService::toSquadPlayer)
                .toList();
    }

    private static SquadPlayer toSquadPlayer(Player player) {
        return new SquadPlayer(player.id().value(), player.position(), !player.isRetired());
    }
}
