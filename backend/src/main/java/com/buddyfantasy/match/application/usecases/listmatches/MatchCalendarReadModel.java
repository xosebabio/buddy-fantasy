package com.buddyfantasy.match.application.usecases.listmatches;

import java.util.List;
import java.util.UUID;

/** Read-side port: the league's matches ordered by matchday in a single query, results included, without sheets. */
public interface MatchCalendarReadModel {

    List<MatchSummaryView> findByLeagueId(UUID leagueId);
}
