package com.buddyfantasy.league.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.league.domain.Invitation;
import org.springframework.stereotype.Repository;

@Repository
class JpaInvitationRepository implements InvitationRepository {

    private final SpringDataInvitationRepository jpa;

    JpaInvitationRepository(SpringDataInvitationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Invitation> findActiveByTokenHash(String tokenHash) {
        return jpa.findByTokenHashAndRevokedAtIsNull(tokenHash).map(InvitationMapper::toDomain);
    }

    @Override
    public Optional<Invitation> findActiveByLeagueId(UUID leagueId) {
        return jpa.findByLeagueIdAndRevokedAtIsNull(leagueId).map(InvitationMapper::toDomain);
    }

    @Override
    public void save(Invitation invitation) {
        jpa.saveAndFlush(InvitationMapper.toEntity(invitation));
    }
}
