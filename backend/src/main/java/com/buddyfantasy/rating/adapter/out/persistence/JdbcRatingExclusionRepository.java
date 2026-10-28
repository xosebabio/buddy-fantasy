package com.buddyfantasy.rating.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.port.out.RatingExclusionRepository;
import com.buddyfantasy.rating.domain.RaterExclusion;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcRatingExclusionRepository implements RatingExclusionRepository {

    private final JdbcClient jdbc;

    @Override
    public List<UUID> findExcludedUserIds(UUID matchId) {
        return jdbc.sql("select user_id from rating_exclusion where match_id = :matchId order by user_id")
                .param("matchId", matchId)
                .query(UUID.class)
                .list();
    }

    @Override
    public void save(RaterExclusion exclusion) {
        jdbc.sql("""
                        insert into rating_exclusion (match_id, user_id, excluded_by, excluded_at)
                        values (:matchId, :userId, :excludedBy, :excludedAt)
                        on conflict (match_id, user_id) do nothing""")
                .param("matchId", exclusion.matchId())
                .param("userId", exclusion.userId())
                .param("excludedBy", exclusion.excludedBy())
                .param("excludedAt", OffsetDateTime.ofInstant(exclusion.excludedAt(), ZoneOffset.UTC))
                .update();
    }
}
