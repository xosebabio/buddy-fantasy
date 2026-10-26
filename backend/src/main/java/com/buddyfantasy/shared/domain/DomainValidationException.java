package com.buddyfantasy.shared.domain;

/** A domain invariant was violated by the given input; maps to HTTP 400. */
public class DomainValidationException extends IllegalArgumentException {

    public DomainValidationException(String message) {
        super(message);
    }
}
