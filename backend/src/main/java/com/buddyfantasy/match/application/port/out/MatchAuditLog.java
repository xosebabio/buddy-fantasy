package com.buddyfantasy.match.application.port.out;

/**
 * Insert-only log of the admin's changes to matches (RNF-04). Command handlers append in the same transaction as
 * the change itself; a change that changes nothing is not logged.
 */
public interface MatchAuditLog {

    void append(MatchAuditEntry entry);
}
