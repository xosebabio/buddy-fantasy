package com.buddyfantasy.lineup.application.usecases.getlockedlineups;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.domain.LockedLineup;

/**
 * A manager's locked lineup with the budget check made at the lock (RF-29): if {@code overBudget} it does not score
 * that match. {@code budget} is null if it was disabled; amounts are in millions. Players ordered by id.
 */
public record LockedLineupView(UUID userId, List<LockedPlayerView> players, BigDecimal totalValue, BigDecimal budget,
                               boolean overBudget) {

    static LockedLineupView from(LockedLineup lineup) {
        return new LockedLineupView(lineup.userId(),
                lineup.players().stream().map(player -> new LockedPlayerView(player.playerId(), player.value()))
                        .toList(),
                lineup.totalValue(), lineup.budget(), lineup.overBudget());
    }
}
