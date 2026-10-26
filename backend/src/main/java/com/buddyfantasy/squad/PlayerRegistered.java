package com.buddyfantasy.squad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.shared.domain.DomainEvent;

/** A player joined the league's squad. {@code market} seeds its value history from {@code initialValue} (RF-10). */
public record PlayerRegistered(UUID leagueId, UUID playerId, BigDecimal initialValue, Instant occurredAt)
        implements DomainEvent {
}
