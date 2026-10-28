package com.buddyfantasy.scoring.application.usecases.getmatchdayscores;

import java.util.Optional;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.scoring.application.port.out.MatchScoreRepository;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.scoring.domain.ScoringError;
import com.buddyfantasy.scoring.domain.Standings;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Reads scoring's copy of the match. Only when it is not scored does it look at the calendar, to tell a match of
 * another league or unknown (404) from one of the league not scored yet (409).
 */
@Component
class GetMatchdayScoresHandler implements QueryHandler<GetMatchdayScoresQuery, MatchdayScoresView> {

    private final LeagueAccess access;
    private final MatchScoreRepository scores;
    private final MatchApi matches;

    GetMatchdayScoresHandler(LeagueAccess access, MatchScoreRepository scores, MatchApi matches) {
        this.access = access;
        this.scores = scores;
        this.matches = matches;
    }

    @Override
    public MatchdayScoresView handle(GetMatchdayScoresQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        Optional<MatchScore> found = scores.findById(query.matchId());
        if (found.isPresent() && !found.get().leagueId().equals(query.leagueId())) {
            throw notInLeague(query);
        }
        MatchScore score = found.filter(MatchScore::scored).orElseThrow(() -> notScored(query));
        return MatchdayScoresView.from(score,
                Standings.matchday(score.matchdayPoints(), access.memberSince(query.leagueId())));
    }

    private RuntimeException notScored(GetMatchdayScoresQuery query) {
        if (matches.findMatches(query.leagueId()).stream().noneMatch(match -> match.id().equals(query.matchId()))) {
            return notInLeague(query);
        }
        return new ConflictException(ScoringError.MATCH_NOT_SCORED,
                "Match " + query.matchId() + " is not scored yet");
    }

    private static NotFoundException notInLeague(GetMatchdayScoresQuery query) {
        return new NotFoundException("Match " + query.matchId() + " not in league " + query.leagueId());
    }
}
