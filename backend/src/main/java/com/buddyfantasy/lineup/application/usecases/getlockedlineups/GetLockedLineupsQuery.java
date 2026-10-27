package com.buddyfantasy.lineup.application.usecases.getlockedlineups;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * Every member's lineup locked for a match (RF-25, RF-27), for the matchday detail (RF-33). Other managers'
 * lineups are only visible once locked; the working lineup stays private ({@code GetMyLineupQuery}).
 */
public record GetLockedLineupsQuery(UUID userId, UUID leagueId, UUID matchId) implements Query<LockedLineupsView> {
}
