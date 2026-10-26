package com.buddyfantasy.league.application.usecases.createleague;

import com.buddyfantasy.league.domain.LeagueId;

/** Result of creating a league; {@code invitationToken} is the raw token of the initial link, shown only once. */
public record CreatedLeague(LeagueId id, String invitationToken) {
}
