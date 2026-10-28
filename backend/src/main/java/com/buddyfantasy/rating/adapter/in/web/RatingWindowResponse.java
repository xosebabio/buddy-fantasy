package com.buddyfantasy.rating.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.rating.application.usecases.listratingwindows.RatingWindowView;
import com.buddyfantasy.rating.domain.RatingStatus;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ventana de valoración de un partido cerrado, según el reloj del servidor.")
record RatingWindowResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio del partido, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "`OPEN` mientras acepta votos; `CLOSED` desde `closesAt`.", example = "OPEN")
        RatingStatus status,
        @Schema(description = "Hasta cuándo acepta votos.", example = "2026-01-12T12:00:00Z")
        Instant closesAt,
        @Schema(description = "Jugadores que se pueden valorar: los del acta.", example = "11")
        int players,
        @Schema(description = "A cuántos de ellos ha valorado el usuario.", example = "4")
        int myRatings) {

    static RatingWindowResponse from(RatingWindowView view) {
        return new RatingWindowResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.status(),
                view.closesAt(), view.players(), view.myRatings());
    }
}
