package com.buddyfantasy.shared.application;

public interface QueryBus {

    <R> R dispatch(Query<R> query);
}
