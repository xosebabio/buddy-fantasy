package com.buddyfantasy.shared.domain;

/** The requested resource does not exist (or must not be distinguishable from missing); maps to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
