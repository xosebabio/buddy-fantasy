package com.buddyfantasy.shared.application;

/** Inbound port of a write use case. Transactions are provided by the {@link CommandBus}. */
public interface CommandHandler<C extends Command<R>, R> {

    R handle(C command);
}
