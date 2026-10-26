package com.buddyfantasy.shared.domain;

import java.time.Instant;

/** Marker for domain events published across modules. */
public interface DomainEvent {

    Instant occurredAt();
}
