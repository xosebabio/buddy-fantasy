package com.buddyfantasy.league.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.Membership;

public interface MembershipRepository {

    Optional<Membership> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    /** Members of the league, oldest first. */
    List<Membership> findByLeagueId(UUID leagueId);

    void save(Membership membership);

    /**
     * Saves the membership unless the user is already a member of its league, also when both saves run at once.
     * Returns whether it was saved.
     */
    boolean saveIfAbsent(Membership membership);

    void delete(Membership membership);
}
