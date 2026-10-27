package com.buddyfantasy.match.application.usecases.listmatchaudit;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/** RNF-04: the league admin's changes to a match, newest first. Still available after the match is deleted. */
public record ListMatchAuditQuery(UUID userId, UUID leagueId, UUID matchId)
        implements Query<List<MatchAuditEntryView>> {
}
