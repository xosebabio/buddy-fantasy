package com.buddyfantasy.match.domain;

/** No "cancelled" status: a suspended match that is not replayed is deleted while still scheduled. */
public enum MatchStatus {
    SCHEDULED,
    CLOSED
}
