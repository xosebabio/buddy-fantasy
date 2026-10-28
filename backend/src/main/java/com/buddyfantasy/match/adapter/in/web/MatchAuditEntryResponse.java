package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.application.usecases.listmatchaudit.MatchAuditEntryView;
import com.buddyfantasy.match.domain.MatchAuditAction;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un cambio del administrador en el partido, con su estado antes y después (RNF-04).")
record MatchAuditEntryResponse(
        @Schema(description = "Id del usuario que hizo el cambio.", example = ApiExamples.USER_ID)
        UUID actorUserId,
        @Schema(description = """
                Tipo de cambio: `SCHEDULED` (alta), `DETAILS_UPDATED` (rival o campo), `RESCHEDULED` (jornada u \
                hora), `SHEET_RECORDED` (acta antes del cierre), `CLOSED`, `CORRECTED` (acta después del cierre) o \
                `DELETED`.""", example = "CORRECTED")
        MatchAuditAction action,
        @Schema(description = "Cuándo se hizo.", example = ApiExamples.LATER_INSTANT)
        Instant occurredAt,
        @Schema(description = "Estado anterior; null en el alta.", nullable = true)
        MatchSnapshotResponse before,
        @Schema(description = "Estado posterior; null en el borrado.", nullable = true)
        MatchSnapshotResponse after) {

    static MatchAuditEntryResponse from(MatchAuditEntryView view) {
        return new MatchAuditEntryResponse(view.actorUserId(), view.action(), view.occurredAt(),
                MatchSnapshotResponse.from(view.before()), MatchSnapshotResponse.from(view.after()));
    }

    @Schema(description = "Estado completo del partido en un momento de su historial.")
    record MatchSnapshotResponse(
            @Schema(description = "Número de jornada.", example = "3")
            int matchday,
            @Schema(description = "Rival.", example = "Atlético Lavadores")
            String opponent,
            @Schema(description = "Si el equipo juega en casa o fuera.", example = "HOME")
            Venue venue,
            @Schema(description = "Fecha y hora de inicio, en UTC.", example = ApiExamples.INSTANT)
            Instant kickoffAt,
            @Schema(description = "Estado del partido.", example = "CLOSED")
            MatchStatus status,
            @Schema(description = "Cuándo se cerró; null si no estaba cerrado.", nullable = true,
                    example = ApiExamples.LATER_INSTANT)
            Instant closedAt,
            @Schema(description = "Resultado; null si no estaba en el acta.", nullable = true)
            MatchResultResponse result,
            @ArraySchema(arraySchema = @Schema(description = "Jugadores del acta; vacía si no había ninguno."))
            List<PlayerPerformanceResponse> performances) {

        /** Null without a snapshot. */
        static MatchSnapshotResponse from(MatchSnapshot snapshot) {
            return snapshot == null ? null : new MatchSnapshotResponse(snapshot.matchday(), snapshot.opponent(),
                    snapshot.venue(), snapshot.kickoffAt(), snapshot.status(), snapshot.closedAt(),
                    MatchResultResponse.from(snapshot.result()),
                    PlayerPerformanceResponse.listOf(snapshot.performances()));
        }
    }
}
