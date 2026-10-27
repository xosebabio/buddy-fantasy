package com.buddyfantasy.shared.domain;

/** The request is valid but conflicts with a business rule in the current state; maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    private final ErrorCode code;

    public ConflictException(String message) {
        this(null, message);
    }

    /** With a {@code code} the client can tell the reason apart. */
    public ConflictException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /** Null when the error has no specific code. */
    public ErrorCode code() {
        return code;
    }
}
