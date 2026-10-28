package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.listmatches.MatchSummaryView;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fila del calendario.")
record MatchSummaryResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID id,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Rival.", example = "Atlético Lavadores")
        String opponent,
        @Schema(description = "Si el equipo juega en casa o fuera.", example = "AWAY")
        Venue venue,
        @Schema(description = "Fecha y hora de inicio, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "`SCHEDULED` hasta que el administrador lo cierra; entonces `CLOSED`.",
                example = "SCHEDULED")
        MatchStatus status,
        @Schema(description = "Cuándo se cerró; null si no está cerrado.", nullable = true,
                example = ApiExamples.LATER_INSTANT)
        Instant closedAt,
        @Schema(description = "Resultado; null si aún no está en el acta, y para los managers hasta el cierre.",
                nullable = true)
        MatchResultResponse result) {

    static MatchSummaryResponse from(MatchSummaryView view) {
        return new MatchSummaryResponse(view.id(), view.matchday(), view.opponent(), view.venue(), view.kickoffAt(),
                view.status(), view.closedAt(), MatchResultResponse.from(view.result()));
    }
}
