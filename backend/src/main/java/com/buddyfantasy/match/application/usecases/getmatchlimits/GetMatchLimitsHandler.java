package com.buddyfantasy.match.application.usecases.getmatchlimits;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.match.application.port.out.MatchLimitsRepository;
import com.buddyfantasy.match.domain.MatchLimits;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GetMatchLimitsHandler implements QueryHandler<GetMatchLimitsQuery, MatchLimitsView> {

    private final LeagueAccess access;
    private final MatchLimitsRepository limits;

    @Override
    public MatchLimitsView handle(GetMatchLimitsQuery query) {
        access.requireAdmin(query.userId(), query.leagueId());
        MatchLimits configured = limits.get();
        return new MatchLimitsView(configured.opponentMaxLength(), configured.matchdayMin(), configured.goalsMax(),
                configured.statCountMax(), configured.sheetPlayersMax());
    }
}
