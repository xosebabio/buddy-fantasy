package com.buddyfantasy.match.application.usecases.recordmatchsheet;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-13, RF-14: the league admin replaces the whole sheet (result and who played with their stats). While the match
 * is scheduled it may be a draft: both goals null and no players yet. On a closed match it is a correction (RF-16).
 * Returns the consistency warnings, which never block saving.
 */
public record RecordMatchSheetCommand(UUID userId, UUID leagueId, UUID matchId, Integer goalsFor,
                                      Integer goalsAgainst, List<PerformanceLine> performances)
        implements Command<List<MatchSheetWarning>> {
}
