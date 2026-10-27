package com.buddyfantasy.lineup.application.usecases.savelineup;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.application.usecases.getmylineup.MyLineupView;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-24, RF-26, RF-29: a league member (the admin plays too) replaces their working lineup. It counts for the first
 * match whose lock is still ahead; a save is never rejected for being late. Returns the lineup as
 * {@code GetMyLineupQuery} does.
 */
public record SaveLineupCommand(UUID userId, UUID leagueId, List<UUID> playerIds) implements Command<MyLineupView> {
}
