package com.buddyfantasy.rating.application.usecases.getmatchratings;

import java.time.Clock;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.rating.application.RatingWindowLookup;
import com.buddyfantasy.rating.application.port.out.RatingExclusionRepository;
import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads only the member's own votes: the others' are never loaded, so they cannot leak (RF-21). */
@Component
@RequiredArgsConstructor
class GetMatchRatingsHandler implements QueryHandler<GetMatchRatingsQuery, MatchRatingsView> {

    private final LeagueAccess access;
    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final RatingExclusionRepository exclusions;
    private final RatingWindowLookup lookup;
    private final Clock clock;

    @Override
    public MatchRatingsView handle(GetMatchRatingsQuery query) {
        LeagueRole role = access.roleOf(query.userId(), query.leagueId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + query.leagueId()));
        RatingWindow window = lookup.require(windows.findById(query.matchId()), query.leagueId(), query.matchId());
        return MatchRatingsView.from(window, votes.findMine(query.matchId(), query.userId()),
                exclusions.findExcludedUserIds(query.matchId()), role == LeagueRole.ADMIN, clock.instant());
    }
}
