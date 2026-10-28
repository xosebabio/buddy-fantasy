package com.buddyfantasy.scoring.domain;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainValidationException;

/**
 * scoring's copy of a manager's locked lineup (RF-27). If {@code overBudget}, it scores 0 that match (RF-29). Its
 * players may have been retired since: they score what the sheet says, which is nothing if they did not play.
 */
public record ManagerLineup(UUID userId, List<UUID> playerIds, boolean overBudget) {

    public ManagerLineup {
        if (userId == null) {
            throw new DomainValidationException("User is required");
        }
        playerIds = List.copyOf(playerIds);
        if (new HashSet<>(playerIds).size() != playerIds.size()) {
            throw new DomainValidationException("A player appears twice in the lineup of user " + userId);
        }
    }
}
