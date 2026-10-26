package com.buddyfantasy.shared.application;

/** Single entry point to the application layer: {@code dispatch} writes, {@code ask} reads. */
public interface CommandBus {

    <R> R dispatch(Command<R> command);

    /** Runs a query in a read-only transaction. */
    <R> R ask(Query<R> query);
}
