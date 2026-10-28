package com.buddyfantasy.scoring.application.usecases.getmatchdayscores;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.buddyfantasy.scoring.domain.ManagerLineup;
import com.buddyfantasy.scoring.domain.MatchScore;
import com.buddyfantasy.scoring.domain.MatchdayStanding;
import com.buddyfantasy.scoring.domain.PlayerScore;

/**
 * A scored match: the standings of the members who already were ones at kickoff, sorted by points, and every player
 * in the sheet, lined up or not, sorted by points and then by id. {@code scoredAt} is the last time its points
 * changed.
 */
public record MatchdayScoresView(UUID matchId, int matchday, Instant kickoffAt, Instant scoredAt,
                                 List<MatchdayStandingView> standings, List<MatchdayPlayerView> players) {

    static MatchdayScoresView from(MatchScore score, List<MatchdayStanding> standings) {
        Map<UUID, ManagerLineup> lineups = score.lineups().stream()
                .collect(Collectors.toMap(ManagerLineup::userId, Function.identity()));
        return new MatchdayScoresView(score.matchId(), score.matchday(), score.kickoffAt(), score.scoredAt(),
                standings.stream()
                        .map(standing -> MatchdayStandingView.from(standing, lineups.get(standing.userId())))
                        .toList(),
                score.playerScores().stream()
                        .sorted(Comparator.comparingInt(PlayerScore::points).reversed()
                                .thenComparing(PlayerScore::playerId))
                        .map(MatchdayPlayerView::from)
                        .toList());
    }
}
