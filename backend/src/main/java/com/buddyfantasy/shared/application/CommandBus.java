package com.buddyfantasy.shared.application;

public interface CommandBus {

    <R> R dispatch(Command<R> command);
}
