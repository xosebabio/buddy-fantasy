package com.buddyfantasy.squad;

import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;

/** A player of the league's real team as seen by other modules: current position and whether it is still active. */
public record SquadPlayer(UUID id, Position position, boolean active) {
}
