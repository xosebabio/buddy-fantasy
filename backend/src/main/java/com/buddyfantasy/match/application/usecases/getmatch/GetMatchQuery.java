package com.buddyfantasy.match.application.usecases.getmatch;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/** Any league member sees a match; its sheet only once closed, unless they are the league admin. */
public record GetMatchQuery(UUID userId, UUID leagueId, UUID matchId) implements Query<MatchView> {
}
