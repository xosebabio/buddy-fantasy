package com.buddyfantasy.shared.domain;

/** The user lacks the per-league role the action requires; maps to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
