package com.buddyfantasy.league.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.Membership;

public interface MembershipRepository {

    Optional<Membership> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    void save(Membership membership);
}
