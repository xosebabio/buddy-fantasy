package com.buddyfantasy.scoring.application.usecases.getmatchdayscores;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * The standings of a scored match (RF-32), with the points of every player in its sheet. 404 if the match is not of
 * the league, 409 ({@code MATCH_NOT_SCORED}) if it is but has no points yet.
 */
public record GetMatchdayScoresQuery(UUID userId, UUID leagueId, UUID matchId)
        implements Query<MatchdayScoresView> {
}
