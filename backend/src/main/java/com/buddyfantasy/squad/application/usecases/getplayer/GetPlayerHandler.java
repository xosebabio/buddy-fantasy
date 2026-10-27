package com.buddyfantasy.squad.application.usecases.getplayer;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.PlayerId;
import org.springframework.stereotype.Component;

@Component
class GetPlayerHandler implements QueryHandler<GetPlayerQuery, PlayerView> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ImageStorage images;

    GetPlayerHandler(LeagueAccess access, PlayerRepository players, ImageStorage images) {
        this.access = access;
        this.players = players;
        this.images = images;
    }

    @Override
    public PlayerView handle(GetPlayerQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        return players.findByLeagueIdAndId(query.leagueId(), new PlayerId(query.playerId()))
                .map(player -> PlayerView.from(player, images))
                .orElseThrow(() -> new NotFoundException("Player " + query.playerId() + " not in league " + query.leagueId()));
    }
}
