package com.buddyfantasy.league.application;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class LeagueAccessService implements LeagueAccess {

    private final MembershipRepository memberships;

    LeagueAccessService(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    public Optional<LeagueRole> roleOf(UUID userId, UUID leagueId) {
        return memberships.findByLeagueIdAndUserId(leagueId, userId).map(Membership::role);
    }

    @Override
    public void requireMember(UUID userId, UUID leagueId) {
        requireMemberSince(userId, leagueId);
    }

    @Override
    public Instant requireMemberSince(UUID userId, UUID leagueId) {
        return memberships.findByLeagueIdAndUserId(leagueId, userId)
                .map(Membership::joinedAt)
                .orElseThrow(() -> new AccessDeniedException("Not a member of league " + leagueId));
    }

    @Override
    public void requireAdmin(UUID userId, UUID leagueId) {
        if (roleOf(userId, leagueId).filter(role -> role == LeagueRole.ADMIN).isEmpty()) {
            throw new AccessDeniedException("Not an admin of league " + leagueId);
        }
    }

    @Override
    public Map<UUID, Instant> memberSince(UUID leagueId) {
        // leaving deletes the membership and joining again creates a new one, so joinedAt is the last join
        return memberships.findByLeagueId(leagueId).stream()
                .collect(Collectors.toUnmodifiableMap(Membership::userId, Membership::joinedAt));
    }
}
