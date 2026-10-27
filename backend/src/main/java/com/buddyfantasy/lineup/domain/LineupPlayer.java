package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;

/** A player of the league as the lineup rules see it: current position, status and current value (in millions). */
public record LineupPlayer(UUID id, Position position, boolean active, BigDecimal currentValue) {
}
