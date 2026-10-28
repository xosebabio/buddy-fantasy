package com.buddyfantasy.lineup.application.usecases.getlockedlineups;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.lineup.domain.MatchLineupLock;
import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads the materialized lock in one query. Only when the match is not locked does it look at the calendar, to tell
 * a match of another league or unknown (404) from one not locked yet (409).
 */
@Component
@RequiredArgsConstructor
class GetLockedLineupsHandler implements QueryHandler<GetLockedLineupsQuery, LockedLineupsView> {

    private final LeagueAccess access;
    private final MatchLineupLockRepository locks;
    private final MatchApi matches;

    @Override
    public LockedLineupsView handle(GetLockedLineupsQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        MatchLineupLock lock = locks.findByMatchId(query.matchId()).orElse(null);
        if (lock != null) {
            if (!lock.leagueId().equals(query.leagueId())) {
                throw notInLeague(query);
            }
            return LockedLineupsView.from(lock);
        }
        if (matches.findMatches(query.leagueId()).stream().noneMatch(match -> match.id().equals(query.matchId()))) {
            throw notInLeague(query);
        }
        throw new ConflictException("The lineups of match " + query.matchId() + " are not locked yet");
    }

    private static NotFoundException notInLeague(GetLockedLineupsQuery query) {
        return new NotFoundException("Match " + query.matchId() + " not in league " + query.leagueId());
    }
}
