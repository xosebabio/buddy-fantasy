package com.buddyfantasy.squad.application.usecases.updateplayer;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.application.Command;

/** RF-09, RF-10: full replacement of a player's editable fields by the league admin. */
public record UpdatePlayerCommand(UUID userId, UUID leagueId, UUID playerId, String name, Integer shirtNumber,
                                  Position position, BigDecimal initialValue) implements Command<Void> {
}
