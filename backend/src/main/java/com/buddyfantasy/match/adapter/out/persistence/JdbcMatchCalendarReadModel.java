package com.buddyfantasy.match.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.listmatches.MatchCalendarReadModel;
import com.buddyfantasy.match.application.usecases.listmatches.MatchSummaryView;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcMatchCalendarReadModel implements MatchCalendarReadModel {

    private final JdbcClient jdbc;

    @Override
    public List<MatchSummaryView> findByLeagueId(UUID leagueId) {
        return jdbc.sql("""
                        select id, matchday, opponent, venue, kickoff_at, status, closed_at, goals_for, goals_against
                        from match_match
                        where league_id = :leagueId
                        order by matchday""")
                .param("leagueId", leagueId)
                .query((rs, i) -> new MatchSummaryView(
                        rs.getObject("id", UUID.class), rs.getInt("matchday"), rs.getString("opponent"),
                        Venue.valueOf(rs.getString("venue")), instant(rs, "kickoff_at"),
                        MatchStatus.valueOf(rs.getString("status")), instant(rs, "closed_at"), result(rs)))
                .list();
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static MatchResult result(ResultSet rs) throws SQLException {
        Integer goalsFor = rs.getObject("goals_for", Integer.class);
        return goalsFor == null ? null : new MatchResult(goalsFor, rs.getInt("goals_against"));
    }
}
