package com.buddyfantasy.league.application.usecases.getleaguelimits;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.domain.LeagueLimits;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GetLeagueLimitsHandler implements QueryHandler<GetLeagueLimitsQuery, LeagueLimitsView> {

    private final LeagueAccess access;
    private final LeagueLimitsRepository limits;

    @Override
    public LeagueLimitsView handle(GetLeagueLimitsQuery query) {
        access.requireAdmin(query.userId(), query.leagueId());
        LeagueLimits configured = limits.get();
        return new LeagueLimitsView(configured.lineupLockHoursMax());
    }
}
