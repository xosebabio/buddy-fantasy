package com.buddyfantasy.shared.domain;

/** A domain invariant was violated by the given input; maps to HTTP 400. */
public class DomainValidationException extends IllegalArgumentException {

    private final ErrorCode code;

    public DomainValidationException(String message) {
        this(null, message);
    }

    /** With a {@code code} the client can tell the reason apart. */
    public DomainValidationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /** Null when the error has no specific code. */
    public ErrorCode code() {
        return code;
    }
}
