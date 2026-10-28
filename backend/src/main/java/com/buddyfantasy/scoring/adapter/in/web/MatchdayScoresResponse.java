package com.buddyfantasy.scoring.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.MatchdayPlayerView;
import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.MatchdayScoresView;
import com.buddyfantasy.scoring.application.usecases.getmatchdayscores.MatchdayStandingView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Puntos de un partido puntuado: la clasificación de la jornada y los puntos de cada jugador.")
record MatchdayScoresResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio del partido, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "Última vez que cambiaron sus puntos (el cierre o la última corrección).",
                example = ApiExamples.LATER_INSTANT)
        Instant scoredAt,
        @ArraySchema(arraySchema = @Schema(description = "Clasificación de la jornada, por puntos: los miembros que ya "
                + "lo eran al empezar el partido."))
        List<MatchdayStandingResponse> standings,
        @ArraySchema(arraySchema = @Schema(description = "Todos los jugadores del acta, alineados o no, por puntos y "
                + "después por id."))
        List<MatchdayPlayerResponse> players) {

    static MatchdayScoresResponse from(MatchdayScoresView view) {
        return new MatchdayScoresResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.scoredAt(),
                view.standings().stream().map(MatchdayStandingResponse::from).toList(),
                view.players().stream().map(MatchdayPlayerResponse::from).toList());
    }

    @Schema(description = "Fila de la clasificación de una jornada.")
    record MatchdayStandingResponse(
            @Schema(description = "Posición; los empatados a puntos la comparten.", example = "2")
            int position,
            @Schema(description = "Id del manager.", example = ApiExamples.USER_ID)
            UUID userId,
            @Schema(description = "Puntos en la jornada: los de sus jugadores alineados; 0 sin alineación bloqueada o "
                    + "fuera de presupuesto (RF-29, RF-31).", example = "24")
            int points,
            @Schema(description = "Si su alineación superaba el presupuesto al bloquearse.", example = "false")
            boolean overBudget,
            @ArraySchema(arraySchema = @Schema(description = "Su alineación bloqueada, por id; vacía si no tenía."),
                    schema = @Schema(example = ApiExamples.PLAYER_ID))
            List<UUID> playerIds) {

        static MatchdayStandingResponse from(MatchdayStandingView view) {
            return new MatchdayStandingResponse(view.position(), view.userId(), view.points(), view.overBudget(),
                    view.playerIds());
        }
    }

    @Schema(description = "Jugador del acta con sus puntos en el partido (RF-30).")
    record MatchdayPlayerResponse(
            @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
            UUID id,
            @Schema(description = "Posición con la que jugó.", example = "FORWARD")
            Position position,
            @Schema(description = "Puntos por acciones, según la tabla de la liga congelada al puntuar.",
                    example = "7")
            int actionPoints,
            @Schema(description = "Puntos por valoración: la media convertida, o la nota por defecto de la liga si no "
                    + "llegó al mínimo de votos; 0 si tampoco la hay (RF-22, RF-23).",
                    example = "0")
            int ratingPoints,
            @Schema(description = "Total: acciones más valoración.", example = "7")
            int points) {

        static MatchdayPlayerResponse from(MatchdayPlayerView view) {
            return new MatchdayPlayerResponse(view.id(), view.position(), view.actionPoints(), view.ratingPoints(),
                    view.points());
        }
    }
}
