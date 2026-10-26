package com.buddyfantasy.league.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import org.springframework.stereotype.Repository;

@Repository
class JpaMembershipRepository implements MembershipRepository {

    private final SpringDataMembershipRepository jpa;

    JpaMembershipRepository(SpringDataMembershipRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Membership> findByLeagueIdAndUserId(UUID leagueId, UUID userId) {
        return jpa.findByLeagueIdAndUserId(leagueId, userId).map(MembershipMapper::toDomain);
    }

    @Override
    public void save(Membership membership) {
        jpa.save(MembershipMapper.toEntity(membership));
    }

    @Override
    public List<Membership> findByLeagueId(UUID leagueId) {
        return jpa.findByLeagueIdOrderByJoinedAtAsc(leagueId).stream().map(MembershipMapper::toDomain).toList();
    }

    @Override
    public void delete(Membership membership) {
        jpa.deleteById(membership.id());
    }
}
