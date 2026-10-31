package com.buddyfantasy.squad.application.usecases.getsquadlimits;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.domain.SquadLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GetSquadLimitsHandler implements QueryHandler<GetSquadLimitsQuery, SquadLimitsView> {

    private final LeagueAccess access;
    private final SquadLimitsRepository limits;

    @Override
    public SquadLimitsView handle(GetSquadLimitsQuery query) {
        access.requireAdmin(query.userId(), query.leagueId());
        SquadLimits configured = limits.get();
        return new SquadLimitsView(configured.nameMaxLength(), configured.shirtNumberMin(),
                configured.shirtNumberMax());
    }
}
