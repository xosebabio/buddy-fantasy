package com.buddyfantasy.match.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.Matchday;

public interface MatchRepository {

    /** Always scoped by league, so a match id from another league is never found (no IDOR). Loads the sheet too. */
    Optional<Match> findByLeagueIdAndId(UUID leagueId, MatchId id);

    boolean existsMatchday(UUID leagueId, Matchday matchday);

    boolean existsKickoff(UUID leagueId, Instant kickoffAt);

    /**
     * Saves the match with its whole sheet and flushes immediately. Throws
     * {@link com.buddyfantasy.shared.domain.ConflictException} if the matchday or the kickoff is already taken in the
     * league (safety net for concurrent requests).
     */
    void save(Match match);

    /** Physical delete of the match and its sheet; its audit entries stay. */
    void delete(Match match);
}
