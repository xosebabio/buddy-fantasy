package com.buddyfantasy.match.application.usecases.getmatch;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchSheet;
import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;

/**
 * Match detail. {@code result} and {@code performances} are null for managers until the match is closed, and
 * {@code result} also while the admin has not recorded it. {@code warnings} are codes for the admin only (null for
 * managers); they never block saving or closing.
 */
public record MatchView(UUID id, int matchday, String opponent, Venue venue, Instant kickoffAt, MatchStatus status,
                        Instant closedAt, MatchResult result, List<PlayerPerformanceData> performances,
                        List<MatchSheetWarning> warnings) {

    public static MatchView from(Match match, boolean admin) {
        boolean sheetVisible = admin || match.isClosed();
        MatchSheet sheet = match.sheet();
        return new MatchView(match.id().value(), match.matchday().value(), match.opponent(), match.venue(),
                match.kickoffAt(), match.status(), match.closedAt(),
                sheetVisible ? sheet.result() : null,
                sheetVisible ? sheet.performancesData() : null,
                admin ? sheet.warnings() : null);
    }
}
