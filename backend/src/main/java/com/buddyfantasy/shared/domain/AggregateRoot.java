package com.buddyfantasy.shared.domain;

import java.util.ArrayList;
import java.util.List;

/** Base type for aggregates; collects domain events until the handler publishes them. */
public abstract class AggregateRoot {

    private final List<DomainEvent> events = new ArrayList<>();

    protected void register(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }
}
