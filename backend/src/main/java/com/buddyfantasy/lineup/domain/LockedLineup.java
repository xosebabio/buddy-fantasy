package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.LockedLineupData;

/**
 * A manager's lineup as locked for a match: the version that counted, its players with their values at the lock,
 * and the budget check made then. {@code budget} is null if the budget was disabled. If {@code overBudget}, the
 * lineup does not score that match (RF-29).
 */
public record LockedLineup(UUID userId, UUID versionId, List<LockedPlayer> players, BigDecimal totalValue,
        BigDecimal budget, boolean overBudget) {

    public LockedLineup {
        players = List.copyOf(players);
    }

    public LockedLineupData toData() {
        return new LockedLineupData(userId, players.stream().map(LockedPlayer::playerId).toList(), totalValue,
                overBudget);
    }
}
