package com.buddyfantasy.lineup.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** A player of a locked lineup with his value at the lock (in millions), frozen against later market changes. */
public record LockedPlayer(UUID playerId, BigDecimal value) {
}
