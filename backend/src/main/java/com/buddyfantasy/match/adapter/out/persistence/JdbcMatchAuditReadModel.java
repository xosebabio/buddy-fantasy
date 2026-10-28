package com.buddyfantasy.match.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.application.usecases.listmatchaudit.MatchAuditEntryView;
import com.buddyfantasy.match.application.usecases.listmatchaudit.MatchAuditReadModel;
import com.buddyfantasy.match.domain.MatchAuditAction;
import com.buddyfantasy.shared.domain.UnexpectedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads the JSONB snapshots written by {@link JdbcMatchAuditLog} with the same {@link ObjectMapper}. */
@Repository
@RequiredArgsConstructor
class JdbcMatchAuditReadModel implements MatchAuditReadModel {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    @Override
    public List<MatchAuditEntryView> findByLeagueIdAndMatchId(UUID leagueId, UUID matchId) {
        return jdbc.sql("""
                        select actor_user_id, action, occurred_at, before, after
                        from match_audit_entry
                        where league_id = :leagueId and match_id = :matchId
                        order by occurred_at desc""")
                .param("leagueId", leagueId)
                .param("matchId", matchId)
                .query((rs, i) -> new MatchAuditEntryView(
                        rs.getObject("actor_user_id", UUID.class), MatchAuditAction.valueOf(rs.getString("action")),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(),
                        snapshot(rs.getString("before")), snapshot(rs.getString("after"))))
                .list();
    }

    private MatchSnapshot snapshot(String value) {
        if (value == null) {
            return null;
        }
        try {
            return json.readValue(value, MatchSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Cannot read match snapshot", e);
        }
    }
}
