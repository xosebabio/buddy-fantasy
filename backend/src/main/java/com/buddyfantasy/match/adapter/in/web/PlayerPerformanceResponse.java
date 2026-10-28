package com.buddyfantasy.match.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.match.PlayerPerformanceData;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estadísticas de un jugador en el acta.")
record PlayerPerformanceResponse(
        @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
        UUID playerId,
        @Schema(description = "Posición con la que jugó: la que tenía en la plantilla al añadirlo al acta.",
                example = "FORWARD")
        Position position,
        @Schema(description = "Goles normales, sin contar los de penalti.", example = "1")
        int goals,
        @Schema(description = "Goles de penalti.", example = "1")
        int penaltyGoals,
        @Schema(description = "Penaltis parados.", example = "0")
        int penaltiesSaved,
        @Schema(description = "Penaltis fallados.", example = "0")
        int penaltiesMissed,
        @Schema(description = "Asistencias.", example = "1")
        int assists,
        @Schema(description = "Goles en propia puerta.", example = "0")
        int ownGoals,
        @Schema(description = "Tarjeta amarilla.", example = "true")
        boolean yellowCard,
        @Schema(description = "Doble amarilla (expulsión).", example = "false")
        boolean doubleYellow,
        @Schema(description = "Roja directa.", example = "false")
        boolean redCard) {

    /** Null without performances. */
    static List<PlayerPerformanceResponse> listOf(List<PlayerPerformanceData> performances) {
        return performances == null ? null : performances.stream()
                .map(p -> new PlayerPerformanceResponse(p.playerId(), p.position(), p.goals(), p.penaltyGoals(),
                        p.penaltiesSaved(), p.penaltiesMissed(), p.assists(), p.ownGoals(), p.yellowCard(),
                        p.doubleYellow(), p.redCard()))
                .toList();
    }
}
