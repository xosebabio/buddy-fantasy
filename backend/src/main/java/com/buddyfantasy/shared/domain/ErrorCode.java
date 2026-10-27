package com.buddyfantasy.shared.domain;

/**
 * Machine-readable reason of a business error, sent to the client as the {@code code} property of the problem
 * detail. Implemented by an enum per module, so {@link #name()} is the code.
 */
public interface ErrorCode {

    String name();
}
