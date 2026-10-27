package com.buddyfantasy.league;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Public API of the league module: per-league role checks and members. There are no global roles. */
public interface LeagueAccess {

    /** Role of the user in the league, empty if not a member. */
    Optional<LeagueRole> roleOf(UUID userId, UUID leagueId);

    /** Throws {@link org.springframework.security.access.AccessDeniedException} (403) if not a member. */
    void requireMember(UUID userId, UUID leagueId);

    /** Throws {@link org.springframework.security.access.AccessDeniedException} (403) if not ADMIN of the league. */
    void requireAdmin(UUID userId, UUID leagueId);

    /** Ids of the league's current members, admin included (they also play as manager). Empty if unknown. */
    Set<UUID> memberIds(UUID leagueId);
}
