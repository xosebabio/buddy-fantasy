package com.buddyfantasy.lineup.application.usecases.getmylineup;

import java.math.BigDecimal;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.lineup.domain.LineupPlayer;

/** A player of the working lineup with his current position, status and value (in millions). */
public record LineupPlayerView(UUID id, Position position, boolean active, BigDecimal value) {

    static LineupPlayerView from(LineupPlayer player) {
        return new LineupPlayerView(player.id(), player.position(), player.active(), player.currentValue());
    }
}
