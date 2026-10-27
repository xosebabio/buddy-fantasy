package com.buddyfantasy.lineup.application.usecases.getlockedlineups;

import java.math.BigDecimal;
import java.util.UUID;

/** A player of a locked lineup with his value at the lock (in millions), not the current one. */
public record LockedPlayerView(UUID id, BigDecimal value) {
}
