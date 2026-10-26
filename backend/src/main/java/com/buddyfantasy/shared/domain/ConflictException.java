package com.buddyfantasy.shared.domain;

/** The request is valid but conflicts with a business rule in the current state; maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
