package com.buddyfantasy.squad;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;

/**
 * A player of the league's real team as seen by other modules: current position, whether it is still active and
 * the initial value set by the admin (in millions). The current market value belongs to {@code market}.
 */
public record SquadPlayer(UUID id, Position position, boolean active, BigDecimal initialValue) {
}
