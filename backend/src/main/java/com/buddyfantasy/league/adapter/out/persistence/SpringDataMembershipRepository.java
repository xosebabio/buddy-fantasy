package com.buddyfantasy.league.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMembershipRepository extends JpaRepository<MembershipEntity, UUID> {

    Optional<MembershipEntity> findByLeagueIdAndUserId(UUID leagueId, UUID userId);

    List<MembershipEntity> findByLeagueIdOrderByJoinedAtAsc(UUID leagueId);
}
