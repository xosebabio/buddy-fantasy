package com.buddyfantasy.match.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.match.application.usecases.recordmatchsheet.PerformanceLine;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Missing counters are 0 and missing cards false. The position is not sent: it comes from the squad. */
@Schema(description = """
        Estadísticas de un jugador en el partido. Los contadores que faltan valen 0 y las tarjetas, `false`. La \
        posición no se envía: se toma de la plantilla al añadirlo al acta y se conserva en las correcciones.""")
record PlayerPerformanceRequest(
        @Schema(description = "Id de un jugador de la plantilla. Uno dado de baja solo puede seguir si ya estaba en "
                + "el acta.", example = ApiExamples.PLAYER_ID)
        @NotNull UUID playerId,
        @Schema(description = "Goles normales, sin contar los de penalti. De 0 a `match.limits.stat-count-max`.",
                minimum = "0", example = "1")
        int goals,
        @Schema(description = "Goles de penalti. De 0 a `match.limits.stat-count-max`.", minimum = "0",
                example = "1")
        int penaltyGoals,
        @Schema(description = "Penaltis parados. De 0 a `match.limits.stat-count-max`.", minimum = "0",
                example = "0")
        int penaltiesSaved,
        @Schema(description = "Penaltis fallados. De 0 a `match.limits.stat-count-max`.", minimum = "0",
                example = "0")
        int penaltiesMissed,
        @Schema(description = "Asistencias. De 0 a `match.limits.stat-count-max`.", minimum = "0", example = "1")
        int assists,
        @Schema(description = "Goles en propia puerta. De 0 a `match.limits.stat-count-max`.", minimum = "0",
                example = "0")
        int ownGoals,
        @Schema(description = "Tarjeta amarilla.", example = "true")
        boolean yellowCard,
        @Schema(description = "Doble amarilla (expulsión). Excluye `yellowCard` y `redCard`: vale −3 en total.",
                example = "false")
        boolean doubleYellow,
        @Schema(description = "Roja directa.", example = "false")
        boolean redCard) {

    PerformanceLine toLine() {
        return new PerformanceLine(playerId, goals, penaltyGoals, penaltiesSaved, penaltiesMissed, assists, ownGoals,
                yellowCard, doubleYellow, redCard);
    }
}
