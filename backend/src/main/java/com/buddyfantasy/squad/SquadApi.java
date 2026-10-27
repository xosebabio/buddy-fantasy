package com.buddyfantasy.squad;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Public API of the squad module: batch lookup of the league's players for other modules. */
public interface SquadApi {

    /**
     * One query for all ids, retired players included. Ids that are unknown or belong to another league are absent
     * from the result.
     */
    List<SquadPlayer> findPlayers(UUID leagueId, Collection<UUID> playerIds);
}
