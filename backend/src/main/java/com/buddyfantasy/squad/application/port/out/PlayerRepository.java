package com.buddyfantasy.squad.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.ShirtNumber;

public interface PlayerRepository {

    /** Always scoped by league, so a player id from another league is never found (no IDOR). */
    Optional<Player> findByLeagueIdAndId(UUID leagueId, PlayerId id);

    /** Every player of the league in a single query; retired ones only when asked for. */
    List<Player> findByLeagueId(UUID leagueId, boolean includeRetired);

    /** The given players of the league in a single query, retired ones included; ids of other leagues are skipped. */
    List<Player> findByLeagueIdAndIds(UUID leagueId, Collection<PlayerId> ids);

    boolean existsActiveShirtNumber(UUID leagueId, ShirtNumber shirtNumber);

    /**
     * Flushes immediately. Throws {@link com.buddyfantasy.shared.domain.ConflictException} if the shirt number is
     * already taken by an active player (safety net for concurrent requests).
     */
    void save(Player player);
}
