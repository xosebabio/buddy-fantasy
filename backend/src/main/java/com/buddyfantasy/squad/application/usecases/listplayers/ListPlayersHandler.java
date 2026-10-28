package com.buddyfantasy.squad.application.usecases.listplayers;

import java.util.Comparator;
import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
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
        LeagueRole role = access.roleOf(query.userId(), query.leagueId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + query.leagueId()));
        boolean includeRetired = query.includeRetired() && role == LeagueRole.ADMIN;
        return players.findByLeagueId(query.leagueId(), includeRetired).stream()
                .map(player -> PlayerView.from(player, images))
                .sorted(SQUAD_ORDER)
                .toList();
    }
}
