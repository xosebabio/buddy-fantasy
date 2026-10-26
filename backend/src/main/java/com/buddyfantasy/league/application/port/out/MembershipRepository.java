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

    void delete(Membership membership);
}
