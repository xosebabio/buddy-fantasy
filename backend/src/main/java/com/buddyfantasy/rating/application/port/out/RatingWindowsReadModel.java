package com.buddyfantasy.rating.application.port.out;

import java.util.List;
import java.util.UUID;

/** Read-side port for the listing of a league's rating windows. */
public interface RatingWindowsReadModel {

    /** Every window of the league with the member's count of votes, in a single query, in no particular order. */
    List<RatingWindowSummary> findByLeagueId(UUID leagueId, UUID userId);
}
