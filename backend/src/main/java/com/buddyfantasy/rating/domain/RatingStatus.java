package com.buddyfantasy.rating.domain;

import java.time.Instant;

/**
 * Whether a window accepts votes at the server's {@code now} (RF-17). It is {@link #CLOSED} from {@code closesAt},
 * although the job has not closed it yet: the results are only there once the job closes it.
 */
public enum RatingStatus {
    OPEN,
    CLOSED;

    public static RatingStatus of(Instant closesAt, Instant closedAt, Instant now) {
        return closedAt == null && now.isBefore(closesAt) ? OPEN : CLOSED;
    }
}
