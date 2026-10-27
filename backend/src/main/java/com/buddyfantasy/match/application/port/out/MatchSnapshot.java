package com.buddyfantasy.match.application.port.out;

import java.time.Instant;
import java.util.List;

import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;

/** Full state of a match as stored before/after a change in the audit log (RNF-04). {@code result} may be null. */
public record MatchSnapshot(int matchday, String opponent, Venue venue, Instant kickoffAt, MatchStatus status,
                            Instant closedAt, MatchResult result, List<PlayerPerformanceData> performances) {

    public static MatchSnapshot of(Match match) {
        return new MatchSnapshot(match.matchday().value(), match.opponent(), match.venue(), match.kickoffAt(),
                match.status(), match.closedAt(), match.sheet().result(), match.sheet().performancesData());
    }
}
