package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.getmatch.MatchView;
import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Detalle de un partido. Un manager no ve el acta (`result`, `performances`) hasta que se cierra; el \
        administrador la ve siempre, con sus avisos.""")
record MatchResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID id,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Rival.", example = "Atlético Lavadores")
        String opponent,
        @Schema(description = "Si el equipo juega en casa o fuera.", example = "HOME")
        Venue venue,
        @Schema(description = "Fecha y hora de inicio, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "`SCHEDULED` hasta que el administrador lo cierra; entonces `CLOSED`.",
                example = "CLOSED")
        MatchStatus status,
        @Schema(description = "Cuándo se cerró; null si no está cerrado.", nullable = true,
                example = ApiExamples.LATER_INSTANT)
        Instant closedAt,
        @Schema(description = "Resultado; null si aún no está en el acta, y para los managers hasta el cierre.",
                nullable = true)
        MatchResultResponse result,
        @ArraySchema(arraySchema = @Schema(description = "Jugadores del acta; null para los managers hasta el "
                + "cierre.", nullable = true))
        List<PlayerPerformanceResponse> performances,
        @ArraySchema(arraySchema = @Schema(description = "Avisos de coherencia del acta, solo para el administrador "
                + "(null para los managers). Nunca impiden guardar ni cerrar.", nullable = true,
                example = "[\"GOALS_MISMATCH\"]"))
        List<MatchSheetWarning> warnings) {

    static MatchResponse from(MatchView view) {
        return new MatchResponse(view.id(), view.matchday(), view.opponent(), view.venue(), view.kickoffAt(),
                view.status(), view.closedAt(), MatchResultResponse.from(view.result()),
                PlayerPerformanceResponse.listOf(view.performances()), view.warnings());
    }
}
