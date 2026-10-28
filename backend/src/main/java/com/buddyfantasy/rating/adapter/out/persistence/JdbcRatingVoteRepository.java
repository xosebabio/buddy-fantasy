package com.buddyfantasy.rating.adapter.out.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.domain.MyRatings;
import com.buddyfantasy.rating.domain.Vote;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcRatingVoteRepository implements RatingVoteRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    @Override
    public Map<UUID, List<Integer>> findValuesByPlayer(UUID matchId) {
        return jdbc.sql("select player_id, value from rating_vote where match_id = :matchId order by player_id")
                .param("matchId", matchId)
                .query((rs, rowNum) -> Map.entry(rs.getObject("player_id", UUID.class), rs.getInt("value")))
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    @Override
    public MyRatings findMine(UUID matchId, UUID userId) {
        return new MyRatings(matchId, userId, jdbc.sql("""
                        select player_id, value from rating_vote
                        where match_id = :matchId and user_id = :userId
                        order by player_id""")
                .param("matchId", matchId)
                .param("userId", userId)
                .query((rs, rowNum) -> new Vote(rs.getObject("player_id", UUID.class), rs.getInt("value")))
                .list());
    }

    @Override
    public void save(MyRatings ratings, Instant now) {
        List<UUID> playerIds = ratings.votes().stream().map(Vote::playerId).toList();
        if (playerIds.isEmpty()) {
            delete(ratings.matchId(), ratings.userId());
            return;
        }
        jdbc.sql("""
                        delete from rating_vote
                        where match_id = :matchId and user_id = :userId and player_id not in (:playerIds)""")
                .param("matchId", ratings.matchId())
                .param("userId", ratings.userId())
                .param("playerIds", playerIds)
                .update();
        OffsetDateTime updatedAt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        batch.batchUpdate("""
                        insert into rating_vote (match_id, player_id, user_id, value, updated_at)
                        values (?, ?, ?, ?, ?)
                        on conflict (match_id, player_id, user_id) do update
                        set value = excluded.value, updated_at = excluded.updated_at
                        where rating_vote.value <> excluded.value""",
                ratings.votes().stream()
                        .map(vote -> new Object[]{ratings.matchId(), vote.playerId(), ratings.userId(), vote.value(),
                                updatedAt})
                        .toList());
    }

    @Override
    public void delete(UUID matchId, UUID userId) {
        jdbc.sql("delete from rating_vote where match_id = :matchId and user_id = :userId")
                .param("matchId", matchId)
                .param("userId", userId)
                .update();
    }
}
