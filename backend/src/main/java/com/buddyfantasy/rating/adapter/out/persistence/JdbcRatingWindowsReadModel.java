package com.buddyfantasy.rating.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.port.out.RatingWindowSummary;
import com.buddyfantasy.rating.application.port.out.RatingWindowsReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The counts are subqueries on the primary key of {@code rating_player} and the (match, user) index of the votes. */
@Repository
@RequiredArgsConstructor
class JdbcRatingWindowsReadModel implements RatingWindowsReadModel {

    private final JdbcClient jdbc;

    @Override
    public List<RatingWindowSummary> findByLeagueId(UUID leagueId, UUID userId) {
        return jdbc.sql("""
                        select w.match_id, w.matchday, w.kickoff_at, w.closes_at, w.closed_at,
                               (select count(*) from rating_player p where p.match_id = w.match_id) as players,
                               (select count(*) from rating_vote v
                                where v.match_id = w.match_id and v.user_id = :userId) as my_ratings
                        from rating_window w
                        where w.league_id = :leagueId""")
                .param("leagueId", leagueId)
                .param("userId", userId)
                .query(JdbcRatingWindowsReadModel::toSummary)
                .list();
    }

    private static RatingWindowSummary toSummary(ResultSet rs, int rowNum) throws SQLException {
        return new RatingWindowSummary(rs.getObject("match_id", UUID.class), rs.getInt("matchday"),
                instant(rs, "kickoff_at"), instant(rs, "closes_at"), instant(rs, "closed_at"), rs.getInt("players"),
                rs.getInt("my_ratings"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
