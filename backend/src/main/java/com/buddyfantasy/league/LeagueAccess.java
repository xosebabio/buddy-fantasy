package com.buddyfantasy.league;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Public API of the league module: per-league role checks and members. There are no global roles. */
public interface LeagueAccess {

    /** Role of the user in the league, empty if not a member. */
    Optional<LeagueRole> roleOf(UUID userId, UUID leagueId);

    /** Throws {@link org.springframework.security.access.AccessDeniedException} (403) if not a member. */
    void requireMember(UUID userId, UUID leagueId);

    /**
     * {@link #requireMember}, returning since when the user is a member: a member who left (or was removed) and
     * joined again is one only since joining again.
     */
    Instant requireMemberSince(UUID userId, UUID leagueId);

    /** Throws {@link org.springframework.security.access.AccessDeniedException} (403) if not ADMIN of the league. */
    void requireAdmin(UUID userId, UUID leagueId);

    /**
     * The league's current members, admin included (they also play as manager), with since when each one is a
     * member, as {@link #requireMemberSince}. Empty if unknown.
     */
    Map<UUID, Instant> memberSince(UUID leagueId);
}
