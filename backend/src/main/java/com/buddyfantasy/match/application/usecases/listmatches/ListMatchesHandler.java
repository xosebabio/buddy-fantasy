package com.buddyfantasy.match.application.usecases.listmatches;

import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ListMatchesHandler implements QueryHandler<ListMatchesQuery, List<MatchSummaryView>> {

    private final LeagueAccess access;
    private final MatchCalendarReadModel calendar;

    @Override
    public List<MatchSummaryView> handle(ListMatchesQuery query) {
        LeagueRole role = access.roleOf(query.userId(), query.leagueId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + query.leagueId()));
        List<MatchSummaryView> matches = calendar.findByLeagueId(query.leagueId());
        if (role == LeagueRole.ADMIN) {
            return matches;
        }
        return matches.stream()
                .map(match -> match.status() == MatchStatus.CLOSED ? match : match.withoutResult())
                .toList();
    }
}
