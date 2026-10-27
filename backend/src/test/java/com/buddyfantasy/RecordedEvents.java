package com.buddyfantasy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.event.EventListener;

/**
 * The application's own events ({@code com.buddyfantasy}), recorded on the thread that publishes them, also the
 * asynchronous threads of the module listeners. Available in every Spring test (imported by
 * {@link PostgresTestConfig}) and emptied before each one by {@link DatabaseCleaner}, after the listeners of the
 * previous test have finished.
 *
 * <p>Use it instead of {@code @RecordApplicationEvents} for the events published by a module listener: that one records
 * through an inheritable thread local, so a pooled listener thread created during an earlier test keeps recording
 * into that test, and whether an event reaches the current one depends on the order of the tests.
 */
@TestComponent
public class RecordedEvents {

    private final List<Object> events = new CopyOnWriteArrayList<>();

    @EventListener
    void on(Object event) {
        if (event.getClass().getPackageName().startsWith("com.buddyfantasy")) {
            events.add(event);
        }
    }

    public <T> Stream<T> stream(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast);
    }

    /** Every event recorded, in publication order. */
    public List<Object> all() {
        return List.copyOf(events);
    }

    void clear() {
        events.clear();
    }
}
