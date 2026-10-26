package com.buddyfantasy.squad.application.usecases.registerplayer;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.shared.application.Command;
import com.buddyfantasy.squad.domain.PlayerId;

/** RF-09, RF-10: the league admin adds a player with an initial value in millions. Shirt number is optional. */
public record RegisterPlayerCommand(UUID userId, UUID leagueId, String name, Integer shirtNumber, Position position,
                                    BigDecimal initialValue) implements Command<PlayerId> {
}
