package com.buddyfantasy.scoring.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.scoring.application.usecases.getstandings.StandingView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fila de la clasificación general.")
record StandingResponse(
        @Schema(description = "Posición; los empatados a puntos la comparten (1, 1, 3).", example = "1")
        int position,
        @Schema(description = "Id del manager.", example = ApiExamples.USER_ID)
        UUID userId,
        @Schema(description = "Puntos acumulados en las jornadas que cuentan para él.", example = "187")
        int points,
        @Schema(description = "Jornadas puntuadas que cuentan para él: las que empezaron después de unirse a la liga.",
                example = "9")
        int matchdays,
        @Schema(description = "Puntos de la última de esas jornadas; 0 si no hay ninguna.", example = "24")
        int lastMatchdayPoints) {

    static StandingResponse from(StandingView view) {
        return new StandingResponse(view.position(), view.userId(), view.points(), view.matchdays(),
                view.lastMatchdayPoints());
    }
}
