package com.buddyfantasy.shared.domain;

/**
 * A broken invariant or an infrastructure failure the caller cannot fix (unreadable stored data, missing algorithm,
 * misconfigured handler…); maps to HTTP 500 without exposing its message.
 */
public class UnexpectedException extends RuntimeException {

    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
