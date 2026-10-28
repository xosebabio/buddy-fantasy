package com.buddyfantasy.rating.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.rating.application.usecases.getmatchratings.MatchRatingsView;
import com.buddyfantasy.rating.application.usecases.getmatchratings.PlayerRatingView;
import com.buddyfantasy.rating.domain.RatingStatus;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Las valoraciones de un partido vistas por un miembro, según el reloj del servidor (RF-18 a RF-21): sus propios \
        votos y, cuando el job cierra la ventana, la media de cada jugador. Nunca los votos de otro miembro, tampoco \
        para el administrador.""")
record MatchRatingsResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio del partido, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "`OPEN` mientras acepta votos; `CLOSED` desde `closesAt`, aunque los resultados solo "
                + "aparecen cuando el job la cierra.", example = "CLOSED")
        RatingStatus status,
        @Schema(description = "Hasta cuándo acepta votos.", example = "2026-01-12T12:00:00Z")
        Instant closesAt,
        @Schema(description = "Formato de los votos, congelado al abrir la ventana: `SPADES` (0 a 4) o `DECIMAL` (0 a "
                + "10).", example = "SPADES")
        RatingFormat format,
        @Schema(description = "Si el administrador canceló las valoraciones del usuario en este partido (RF-41): "
                + "sus votos se borraron y ya no puede valorarlo.", example = "false")
        boolean excluded,
        @ArraySchema(arraySchema = @Schema(description = "Ids de los miembros cuyas valoraciones del partido canceló el "
                + "administrador (RF-41); solo para el administrador, null para el resto.", nullable = true),
                schema = @Schema(example = ApiExamples.OTHER_USER_ID))
        List<UUID> excludedUserIds,
        @ArraySchema(arraySchema = @Schema(description = "Los jugadores del acta, porteros primero."))
        List<PlayerRatingResponse> players) {

    static MatchRatingsResponse from(MatchRatingsView view) {
        return new MatchRatingsResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.status(),
                view.closesAt(), view.format(), view.excluded(), view.excludedUserIds(),
                view.players().stream().map(PlayerRatingResponse::from).toList());
    }

    @Schema(description = "Jugador del acta con el voto del usuario y, con la ventana cerrada, su resultado.")
    record PlayerRatingResponse(
            @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
            UUID playerId,
            @Schema(description = "Posición con la que jugó.", example = "FORWARD")
            Position position,
            @Schema(description = "Voto del usuario; null si no lo ha valorado.", nullable = true, example = "3")
            Integer myRating,
            @Schema(description = "Votos que contaron; null hasta que se cierra la ventana.", nullable = true,
                    example = "6")
            Integer votes,
            @Schema(description = "Media de los votos, redondeada a 2 decimales solo para mostrarla; null hasta que se "
                    + "cierra la ventana o sin votos.", nullable = true, example = "2.83")
            BigDecimal average,
            @Schema(description = "Puntos por valoración (la media convertida y redondeada, mitad hacia arriba); null "
                    + "hasta que se cierra la ventana.", nullable = true, example = "9")
            Integer ratingPoints) {

        static PlayerRatingResponse from(PlayerRatingView view) {
            return new PlayerRatingResponse(view.playerId(), view.position(), view.myRating(), view.votes(),
                    view.average(), view.ratingPoints());
        }
    }
}
