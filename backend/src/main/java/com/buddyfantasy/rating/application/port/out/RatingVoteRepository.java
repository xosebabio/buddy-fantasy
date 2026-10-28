package com.buddyfantasy.rating.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.rating.domain.MyRatings;

/** The members' votes (RF-18 to RF-21). Anonymous: nothing read here is ever returned to another member. */
public interface RatingVoteRepository {

    /** The values of every vote of the match by player, in a single query; players without votes are absent. */
    Map<UUID, List<Integer>> findValuesByPlayer(UUID matchId);

    /** The member's own votes in the match, in a single query; none if they have not rated it. */
    MyRatings findMine(UUID matchId, UUID userId);

    /**
     * Replaces the member's votes in the match with {@code ratings}: the players missing from them lose the member's
     * vote. A vote whose value changes is stamped with {@code now}; one that does not keep its time.
     */
    void save(MyRatings ratings, Instant now);
}
