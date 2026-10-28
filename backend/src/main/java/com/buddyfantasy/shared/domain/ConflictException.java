package com.buddyfantasy.shared.domain;

/**
 * The request is valid but conflicts with a business rule in the current state; maps to HTTP 409. Always with a
 * {@code code}, so the client can tell the reason apart and explain it.
 */
public class ConflictException extends RuntimeException {

    private final ErrorCode code;

    public ConflictException(ErrorCode code, String message) {
        super(message);
        if (code == null) {
            throw new UnexpectedException("A conflict needs a code: " + message);
        }
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
