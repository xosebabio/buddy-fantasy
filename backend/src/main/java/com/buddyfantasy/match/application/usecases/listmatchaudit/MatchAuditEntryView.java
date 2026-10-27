package com.buddyfantasy.match.application.usecases.listmatchaudit;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.domain.MatchAuditAction;

/** One entry of a match's history. {@code before} is null on scheduling and {@code after} on deletion. */
public record MatchAuditEntryView(UUID actorUserId, MatchAuditAction action, Instant occurredAt,
                                  MatchSnapshot before, MatchSnapshot after) {
}
