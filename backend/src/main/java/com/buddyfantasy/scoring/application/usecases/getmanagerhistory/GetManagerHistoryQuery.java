package com.buddyfantasy.scoring.application.usecases.getmanagerhistory;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * The history of a manager (RF-33) as {@code userId} sees it: every scored match since {@code managerId} joined, and
 * those before in which they had a lineup (they left and came back), marked as outside the standings. 404 if
 * {@code managerId} is not a current member of the league.
 */
public record GetManagerHistoryQuery(UUID userId, UUID leagueId, UUID managerId)
        implements Query<List<ManagerMatchdayView>> {
}
