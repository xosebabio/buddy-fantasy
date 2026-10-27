package com.buddyfantasy.league.adapter.out.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JpaMembershipRepository implements MembershipRepository {

    private final SpringDataMembershipRepository jpa;
    private final JdbcClient jdbc;

    JpaMembershipRepository(SpringDataMembershipRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
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
    public boolean saveIfAbsent(Membership membership) {
        // a concurrent insert of the same member waits for the other transaction and then does nothing
        return jdbc.sql("""
                        insert into league_membership (id, league_id, user_id, role, joined_at)
                        values (:id, :leagueId, :userId, :role, :joinedAt)
                        on conflict (league_id, user_id) do nothing""")
                .param("id", membership.id())
                .param("leagueId", membership.leagueId().value())
                .param("userId", membership.userId())
                .param("role", membership.role().name())
                .param("joinedAt", utc(membership.joinedAt()))
                .update() > 0;
    }

    @Override
    public List<Membership> findByLeagueId(UUID leagueId) {
        return jpa.findByLeagueIdOrderByJoinedAtAsc(leagueId).stream().map(MembershipMapper::toDomain).toList();
    }

    @Override
    public void delete(Membership membership) {
        jpa.deleteById(membership.id());
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
