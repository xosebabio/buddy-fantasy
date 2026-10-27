package com.buddyfantasy.match.application.usecases.listmatchaudit;

import java.util.List;
import java.util.UUID;

/** Read-side port over the audit log, independent of the match table so deleted matches keep their history. */
public interface MatchAuditReadModel {

    /** Entries of the match in the league, newest first, in a single query. Empty if there are none. */
    List<MatchAuditEntryView> findByLeagueIdAndMatchId(UUID leagueId, UUID matchId);
}
