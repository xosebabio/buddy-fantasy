package com.buddyfantasy.rating.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The members' votes (RF-18 to RF-21). Anonymous: nothing read here is ever returned to another member. */
public interface RatingVoteRepository {

    /** The values of every vote of the match by player, in a single query; players without votes are absent. */
    Map<UUID, List<Integer>> findValuesByPlayer(UUID matchId);
}
