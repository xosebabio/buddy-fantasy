package com.buddyfantasy.lineup.application.usecases.getmylineup;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/** RF-24 to RF-29: any league member sees their own working lineup, never another manager's. */
public record GetMyLineupQuery(UUID userId, UUID leagueId) implements Query<MyLineupView> {
}
