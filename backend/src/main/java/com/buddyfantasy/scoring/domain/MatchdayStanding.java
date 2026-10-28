package com.buddyfantasy.scoring.domain;

import java.util.UUID;

/** A row of the standings of one match (RF-32). Tied members share the position. */
public record MatchdayStanding(int position, UUID userId, int points) {
}
