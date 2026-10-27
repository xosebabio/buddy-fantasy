package com.buddyfantasy.lineup.application.usecases.getmylineup;

import java.time.Instant;
import java.util.UUID;

/** The match a save made now counts for, and until when: after {@code lockAt} it counts for the next one. */
public record TargetMatchView(UUID id, int matchday, Instant kickoffAt, Instant lockAt) {
}
