package com.buddyfantasy.rating.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcRatingVoteRepository implements RatingVoteRepository {

    private final JdbcClient jdbc;

    JdbcRatingVoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, List<Integer>> findValuesByPlayer(UUID matchId) {
        return jdbc.sql("select player_id, value from rating_vote where match_id = :matchId order by player_id")
                .param("matchId", matchId)
                .query((rs, rowNum) -> Map.entry(rs.getObject("player_id", UUID.class), rs.getInt("value")))
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }
}
