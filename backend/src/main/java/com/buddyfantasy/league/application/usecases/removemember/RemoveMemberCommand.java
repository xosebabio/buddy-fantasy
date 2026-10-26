package com.buddyfantasy.league.application.usecases.removemember;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** Admin removes a manager from the league. The admin themself cannot be removed. */
public record RemoveMemberCommand(UUID userId, UUID leagueId, UUID memberId) implements Command<Void> {
}
