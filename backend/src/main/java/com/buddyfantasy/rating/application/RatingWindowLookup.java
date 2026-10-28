package com.buddyfantasy.rating.application;

import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.match.MatchApi;
import com.buddyfantasy.rating.domain.RatingError;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The window of a match of the league for the members' use cases. Only when the match has no window does it look at
 * the calendar, to tell a match of another league or unknown (404) from one of the league not closed yet (409).
 */
@Component
@RequiredArgsConstructor
public class RatingWindowLookup {

    private final MatchApi matches;

    /** {@code found} is the match's window as read (or locked) by the caller. */
    public RatingWindow require(Optional<RatingWindow> found, UUID leagueId, UUID matchId) {
        if (found.isPresent()) {
            if (!found.get().leagueId().equals(leagueId)) {
                throw notInLeague(leagueId, matchId);
            }
            return found.get();
        }
        if (matches.findMatches(leagueId).stream().noneMatch(match -> match.id().equals(matchId))) {
            throw notInLeague(leagueId, matchId);
        }
        // also for a closed match whose window its MatchClosed is still opening
        throw new ConflictException(RatingError.MATCH_NOT_CLOSED, "Match " + matchId + " is not closed yet");
    }

    private static NotFoundException notInLeague(UUID leagueId, UUID matchId) {
        return new NotFoundException("Match " + matchId + " not in league " + leagueId);
    }
}
