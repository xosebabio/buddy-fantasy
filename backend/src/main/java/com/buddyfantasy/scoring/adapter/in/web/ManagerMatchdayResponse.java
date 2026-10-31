package com.buddyfantasy.scoring.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;
import com.buddyfantasy.scoring.application.usecases.getmanagerhistory.LinedUpPlayerView;
import com.buddyfantasy.scoring.application.usecases.getmanagerhistory.ManagerMatchdayView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partido puntuado del historial de un manager (RF-33).")
record ManagerMatchdayResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio del partido, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "Sus puntos: 0 sin alineación bloqueada o fuera de presupuesto (RF-29).", example = "24")
        int points,
        @Schema(description = "Si su alineación superaba el presupuesto al bloquearse.", example = "false")
        boolean overBudget,
        @Schema(description = "False si el partido es de antes de su última alta en la liga: ya no cuenta para su "
                + "clasificación.", example = "true")
        boolean inStandings,
        @ArraySchema(arraySchema = @Schema(description = "Los jugadores de su alineación bloqueada, por puntos y "
                + "después por id; vacía sin alineación."))
        List<LinedUpPlayerResponse> players) {

    static ManagerMatchdayResponse from(ManagerMatchdayView view) {
        return new ManagerMatchdayResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.points(),
                view.overBudget(), view.inStandings(),
                view.players().stream().map(LinedUpPlayerResponse::from).toList());
    }

    @Schema(description = "Jugador de una alineación bloqueada con sus puntos en el partido.")
    record LinedUpPlayerResponse(
            @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
            UUID id,
            @Schema(description = "Si jugó: uno que no jugó puntúa 0.", example = "true")
            boolean played,
            @Schema(description = "Posición con la que jugó; null si no jugó.", nullable = true, example = "FORWARD")
            Position position,
            @Schema(description = "Puntos de cada acción que suma o resta, con la acción "
                    + "(`ScoringAction`) como clave; vacío si no jugó.",
                    example = "{\"GOAL\": 3, \"PENALTY_GOAL\": 3, \"YELLOW_CARD\": -1}")
            Map<ScoringAction, Integer> actions,
            @Schema(description = "Puntos por acciones.", example = "5")
            int actionPoints,
            @Schema(description = "Puntos por valoración: la media convertida, o la nota por defecto de la liga si no "
                    + "llegó al mínimo de votos; 0 si tampoco la hay (RF-22, RF-23).",
                    example = "0")
            int ratingPoints,
            @Schema(description = "Total: acciones más valoración.", example = "5")
            int points) {

        static LinedUpPlayerResponse from(LinedUpPlayerView view) {
            return new LinedUpPlayerResponse(view.id(), view.played(), view.position(), view.actions(),
                    view.actionPoints(), view.ratingPoints(), view.points());
        }
    }
}
