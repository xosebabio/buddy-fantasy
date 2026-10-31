package com.buddyfantasy.squad.application.usecases.listplayers;

import java.util.Comparator;
import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ListPlayersHandler implements QueryHandler<ListPlayersQuery, List<PlayerView>> {

    /** Goalkeepers first, then by shirt number (players without one last) and name. */
    private static final Comparator<PlayerView> SQUAD_ORDER = Comparator.comparing(PlayerView::position)
            .thenComparing(PlayerView::shirtNumber, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PlayerView::name);

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ImageStorage images;

    @Override
    public List<PlayerView> handle(ListPlayersQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        return players.findByLeagueId(query.leagueId(), query.includeRetired()).stream()
                .map(player -> PlayerView.from(player, images))
                .sorted(SQUAD_ORDER)
                .toList();
    }
}
