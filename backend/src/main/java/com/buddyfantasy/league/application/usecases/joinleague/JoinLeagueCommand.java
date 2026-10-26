package com.buddyfantasy.league.application.usecases.joinleague;

import java.util.UUID;

import com.buddyfantasy.league.domain.LeagueId;
import com.buddyfantasy.shared.application.Command;

/** RF-06: joins the league as MANAGER using an invitation token. Idempotent for existing members. */
public record JoinLeagueCommand(UUID userId, String token) implements Command<LeagueId> {

    /** The token is a credential: keep it out of logs. */
    @Override
    public String toString() {
        return "JoinLeagueCommand[userId=" + userId + "]";
    }
}
