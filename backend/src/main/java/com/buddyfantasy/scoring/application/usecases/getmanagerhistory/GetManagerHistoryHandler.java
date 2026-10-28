package com.buddyfantasy.scoring.application.usecases.getmanagerhistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

/** One query for the members and two for the manager's matches and lineups' players. */
@Component
class GetManagerHistoryHandler implements QueryHandler<GetManagerHistoryQuery, List<ManagerMatchdayView>> {

    private final LeagueAccess access;
    private final MatchScoreRepository scores;

    GetManagerHistoryHandler(LeagueAccess access, MatchScoreRepository scores) {
        this.access = access;
        this.scores = scores;
    }

    @Override
    public List<ManagerMatchdayView> handle(GetManagerHistoryQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        Instant memberSince = Optional.ofNullable(access.memberSince(query.leagueId()).get(query.managerId()))
                .orElseThrow(() -> new NotFoundException(
                        "Manager " + query.managerId() + " not in league " + query.leagueId()));
        return scores.findManagerMatchdays(query.leagueId(), query.managerId()).stream()
                .filter(matchday -> matchday.lineup() != null || matchday.countsFor(memberSince))
                .map(matchday -> ManagerMatchdayView.from(matchday, matchday.countsFor(memberSince)))
                .toList();
    }
}
