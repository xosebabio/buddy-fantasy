package com.buddyfantasy.league.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    Optional<InvitationEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<InvitationEntity> findByLeagueIdAndRevokedAtIsNull(UUID leagueId);
}
