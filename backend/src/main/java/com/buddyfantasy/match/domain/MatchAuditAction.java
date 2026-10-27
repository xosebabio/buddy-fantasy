package com.buddyfantasy.match.domain;

/** Kind of admin change recorded in the match audit log (RNF-04). */
public enum MatchAuditAction {
    SCHEDULED,
    DETAILS_UPDATED,
    RESCHEDULED,
    SHEET_RECORDED,
    CLOSED,
    CORRECTED,
    DELETED
}
