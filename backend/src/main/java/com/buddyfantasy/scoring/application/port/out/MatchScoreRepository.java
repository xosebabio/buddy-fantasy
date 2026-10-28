package com.buddyfantasy.scoring.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.scoring.domain.MatchdayPoints;

/**
 * scoring's matches with their copies of the sheet and locked lineups and their points. Every write first locks the
 * match's row until the transaction ends, so events of the same match handled at once (a {@code MatchClosed} and the
 * {@code LineupsLocked} it triggers) take turns and the second one sees the copy of the first.
 */
public interface MatchScoreRepository {

    /**
     * Inserts {@code initial} (a match without copies) unless the match is already there, also by a concurrent
     * transaction, then locks it and returns it as stored.
     */
    MatchScore lockOrCreate(MatchScore initial);

    /** Locks the match and returns it with its copies and points; empty if scoring has not heard of it. */
    Optional<MatchScore> lock(UUID matchId);

    /** The match with its copies and points, without locking it, for the views; empty if scoring has not heard of it. */
    Optional<MatchScore> findById(UUID matchId);

    /**
     * The managers' points in every scored match of the league, in a single query, in chronological order. A scored
     * match without lineups is there too, with no points.
     */
    List<MatchdayPoints> findScoredMatchdayPoints(UUID leagueId);

    /** Replaces the stored match, copies and points included, with {@code score}. */
    void save(MatchScore score);

    /** Deletes the match with its copies and points; nothing if it is not there. */
    void delete(UUID matchId);
}
