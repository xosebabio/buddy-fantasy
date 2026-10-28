package com.buddyfantasy.rating.application.usecases.getmatchratings;

import java.time.Clock;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.rating.application.RatingWindowLookup;
import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads only the member's own votes: the others' are never loaded, so they cannot leak (RF-21). */
@Component
@RequiredArgsConstructor
class GetMatchRatingsHandler implements QueryHandler<GetMatchRatingsQuery, MatchRatingsView> {

    private final LeagueAccess access;
    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final RatingWindowLookup lookup;
    private final Clock clock;

    @Override
    public MatchRatingsView handle(GetMatchRatingsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        RatingWindow window = lookup.require(windows.findById(query.matchId()), query.leagueId(), query.matchId());
        return MatchRatingsView.from(window, votes.findMine(query.matchId(), query.userId()), clock.instant());
    }
}
