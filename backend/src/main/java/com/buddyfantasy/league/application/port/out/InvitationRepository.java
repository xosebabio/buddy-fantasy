package com.buddyfantasy.league.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.domain.Invitation;

public interface InvitationRepository {

    Optional<Invitation> findActiveByTokenHash(String tokenHash);

    Optional<Invitation> findActiveByLeagueId(UUID leagueId);

    /** Flushes immediately so a revocation is applied before the replacement is inserted. */
    void save(Invitation invitation);
}
