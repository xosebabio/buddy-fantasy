package com.buddyfantasy.rating.application.port.out;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.domain.RaterExclusion;

/**
 * The members whose ratings of a match the admin cancelled (RF-41). Read after locking the window, so a reader sees
 * an exclusion committed while it waited for the lock.
 */
public interface RatingExclusionRepository {

    /** The excluded members of the match, in a single query, ordered by id. */
    List<UUID> findExcludedUserIds(UUID matchId);

    /** Saves the exclusion unless the member was already excluded from the match. */
    void save(RaterExclusion exclusion);
}
