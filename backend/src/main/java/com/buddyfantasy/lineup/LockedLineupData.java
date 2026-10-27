package com.buddyfantasy.lineup;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A manager's lineup as locked for a match. {@code totalValue} (in millions) uses the values current at the lock;
 * if {@code overBudget}, the manager scores 0 points in that match (RF-29).
 */
public record LockedLineupData(UUID userId, List<UUID> playerIds, BigDecimal totalValue, boolean overBudget) {

    public LockedLineupData {
        playerIds = List.copyOf(playerIds);
    }
}
