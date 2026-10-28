package com.buddyfantasy.scoring.adapter.in.web;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.scoring.application.usecases.getplayerpoints.PlayerMatchdayView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Puntos de un jugador en un partido puntuado (RF-11, RF-30).")
record PlayerMatchdayResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio del partido, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "Posición con la que jugó.", example = "FORWARD")
        Position position,
        @Schema(description = "Puntos de cada acción que suma o resta.",
                example = "{\"GOAL\": 3, \"PENALTY_GOAL\": 3, \"YELLOW_CARD\": -1}")
        Map<ScoringAction, Integer> actions,
        @Schema(description = "Puntos por acciones.", example = "5")
        int actionPoints,
        @Schema(description = "Puntos por valoración; 0 hasta que se combinen las valoraciones (Fase 2).",
                example = "0")
        int ratingPoints,
        @Schema(description = "Total: acciones más valoración.", example = "5")
        int points) {

    static PlayerMatchdayResponse from(PlayerMatchdayView view) {
        return new PlayerMatchdayResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.position(),
                view.actions(), view.actionPoints(), view.ratingPoints(), view.points());
    }
}
