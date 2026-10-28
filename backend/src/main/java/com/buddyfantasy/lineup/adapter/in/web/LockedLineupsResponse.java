package com.buddyfantasy.lineup.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.lineup.application.usecases.getlockedlineups.LockedLineupView;
import com.buddyfantasy.lineup.application.usecases.getlockedlineups.LockedLineupsView;
import com.buddyfantasy.lineup.application.usecases.getlockedlineups.LockedPlayerView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Las alineaciones bloqueadas de un partido, tal y como quedaron al bloquearse (RF-25, RF-27).")
record LockedLineupsResponse(
        @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
        UUID matchId,
        @Schema(description = "Número de jornada.", example = "3")
        int matchday,
        @Schema(description = "Hora de inicio, en UTC.", example = ApiExamples.INSTANT)
        Instant kickoffAt,
        @Schema(description = "Hora de bloqueo: cuentan las alineaciones guardadas antes.",
                example = "2026-01-11T08:00:00Z")
        Instant lockAt,
        @Schema(description = "Cuándo se bloquearon de verdad (el job o el cierre del partido).",
                example = "2026-01-11T08:01:00Z")
        Instant lockedAt,
        @ArraySchema(arraySchema = @Schema(description = """
                Una por manager, por id de usuario. No aparecen quienes no guardaron ninguna antes de `lockAt` ni \
                quienes ya no eran miembros entonces."""))
        List<LockedLineupResponse> lineups) {

    static LockedLineupsResponse from(LockedLineupsView view) {
        return new LockedLineupsResponse(view.matchId(), view.matchday(), view.kickoffAt(), view.lockAt(),
                view.lockedAt(), view.lineups().stream().map(LockedLineupResponse::from).toList());
    }

    @Schema(description = "Alineación bloqueada de un manager, con la comprobación del presupuesto al bloquear "
            + "(RF-29). Importes en millones.")
    record LockedLineupResponse(
            @Schema(description = "Id del manager.", example = ApiExamples.USER_ID)
            UUID userId,
            @ArraySchema(arraySchema = @Schema(description = "Sus jugadores, por id."))
            List<LockedPlayerResponse> players,
            @Schema(description = "Suma del valor de sus jugadores al bloquear.", example = "96")
            BigDecimal totalValue,
            @Schema(description = "Presupuesto al bloquear; null si estaba desactivado.", nullable = true,
                    example = "100")
            BigDecimal budget,
            @Schema(description = "Si superaba el presupuesto: entonces no puntúa en este partido.", example = "false")
            boolean overBudget) {

        static LockedLineupResponse from(LockedLineupView view) {
            return new LockedLineupResponse(view.userId(),
                    view.players().stream().map(LockedPlayerResponse::from).toList(), view.totalValue(),
                    view.budget(), view.overBudget());
        }
    }

    @Schema(description = "Jugador de una alineación bloqueada.")
    record LockedPlayerResponse(
            @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
            UUID id,
            @Schema(description = "Su valor al bloquear (no el actual), en millones.", example = "12.5")
            BigDecimal value) {

        static LockedPlayerResponse from(LockedPlayerView view) {
            return new LockedPlayerResponse(view.id(), view.value());
        }
    }
}
