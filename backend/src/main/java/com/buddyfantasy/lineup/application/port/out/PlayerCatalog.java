package com.buddyfantasy.lineup.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.LineupPlayer;

/** The league's players with their current position, status and value, as the lineup rules need them. */
public interface PlayerCatalog {

    /**
     * One query for all ids, retired players included, keyed by id. Ids that are unknown or belong to another
     * league are absent from the result.
     */
    Map<UUID, LineupPlayer> findPlayers(UUID leagueId, Collection<UUID> playerIds);
}
