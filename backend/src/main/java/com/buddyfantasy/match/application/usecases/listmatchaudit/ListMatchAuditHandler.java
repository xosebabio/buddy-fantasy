package com.buddyfantasy.match.application.usecases.listmatchaudit;

import java.util.List;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Every match has at least its scheduling entry, so no entries means the match never existed in this league (or
 * belongs to another one): {@code 404}, like the other match endpoints.
 */
@Component
class ListMatchAuditHandler implements QueryHandler<ListMatchAuditQuery, List<MatchAuditEntryView>> {

    private final LeagueAccess access;
    private final MatchAuditReadModel auditLog;

    ListMatchAuditHandler(LeagueAccess access, MatchAuditReadModel auditLog) {
        this.access = access;
        this.auditLog = auditLog;
    }

    @Override
    public List<MatchAuditEntryView> handle(ListMatchAuditQuery query) {
        access.requireAdmin(query.userId(), query.leagueId());
        List<MatchAuditEntryView> entries = auditLog.findByLeagueIdAndMatchId(query.leagueId(), query.matchId());
        if (entries.isEmpty()) {
            throw new NotFoundException("Match " + query.matchId() + " not in league " + query.leagueId());
        }
        return entries;
    }
}
