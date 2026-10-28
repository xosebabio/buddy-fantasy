package com.buddyfantasy.match.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.buddyfantasy.match.application.port.out.MatchAuditEntry;
import com.buddyfantasy.match.application.port.out.MatchAuditLog;
import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.shared.domain.UnexpectedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Snapshots are stored as JSONB with the application's {@link ObjectMapper} (ISO-8601 instants). */
@Repository
@RequiredArgsConstructor
class JdbcMatchAuditLog implements MatchAuditLog {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    @Override
    public void append(MatchAuditEntry entry) {
        jdbc.sql("""
                        insert into match_audit_entry
                            (id, league_id, match_id, actor_user_id, action, occurred_at, before, after)
                        values (:id, :leagueId, :matchId, :actorUserId, :action, :occurredAt,
                                cast(:before as jsonb), cast(:after as jsonb))""")
                .param("id", UUID.randomUUID())
                .param("leagueId", entry.leagueId())
                .param("matchId", entry.matchId())
                .param("actorUserId", entry.actorUserId())
                .param("action", entry.action().name())
                .param("occurredAt", OffsetDateTime.ofInstant(entry.occurredAt(), ZoneOffset.UTC))
                .param("before", toJson(entry.before()))
                .param("after", toJson(entry.after()))
                .update();
    }

    private String toJson(MatchSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return json.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Cannot serialize match snapshot", e);
        }
    }
}
