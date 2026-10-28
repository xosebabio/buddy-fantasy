package com.buddyfantasy.lineup.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.lineup.application.usecases.getmylineup.LineupPlayerView;
import com.buddyfantasy.lineup.application.usecases.getmylineup.MyLineupView;
import com.buddyfantasy.lineup.application.usecases.getmylineup.TargetMatchView;
import com.buddyfantasy.lineup.domain.LineupWarning;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Alineación de trabajo del manager según el reloj del servidor (RF-24 a RF-29). Si no la toca, se arrastra a \
        la jornada siguiente. Importes en millones.""")
record MyLineupResponse(
        @Schema(description = "Cuándo la guardó por última vez; null si nunca la ha guardado.", nullable = true,
                example = ApiExamples.INSTANT)
        Instant savedAt,
        @ArraySchema(arraySchema = @Schema(description = "Sus jugadores, porteros primero; vacía si nunca la ha "
                + "guardado."))
        List<LineupPlayerResponse> players,
        @Schema(description = "Partido para el que cuenta guardarla ahora; null si no queda ninguno por jugar.",
                nullable = true)
        TargetMatchResponse targetMatch,
        @Schema(description = "Cambios permitidos por jornada.", example = "2")
        int maxChanges,
        @Schema(description = "Cambios que lleva contra la alineación bloqueada del último partido bloqueado. "
                + "Sustituir a un jugador dado de baja no cuenta.", example = "1")
        int changesUsed,
        @Schema(description = "Cambios que le quedan; null mientras no hay límite (aún no se ha bloqueado ninguna "
                + "alineación suya).", nullable = true, example = "1")
        Integer changesRemaining,
        @Schema(description = "Suma del valor vigente de sus jugadores.", example = "84.5")
        BigDecimal totalValue,
        @Schema(description = "Presupuesto máximo; null si la liga lo tiene desactivado.", nullable = true,
                example = "100")
        BigDecimal budget,
        @ArraySchema(arraySchema = @Schema(description = """
                Problemas causados por cambios posteriores, que nunca impiden nada: `OVER_BUDGET` (con los valores \
                actuales ya no cabe en el presupuesto; si sigue así al bloquear, esa jornada puntúa 0), \
                `RETIRED_PLAYERS` (algún jugador se dio de baja; sustituirlo es gratis) y `SIZE_MISMATCH` (la liga \
                cambió el tamaño de las alineaciones).""", example = "[\"RETIRED_PLAYERS\"]"))
        List<LineupWarning> warnings) {

    static MyLineupResponse from(MyLineupView view) {
        return new MyLineupResponse(view.savedAt(),
                view.players().stream().map(LineupPlayerResponse::from).toList(),
                TargetMatchResponse.from(view.targetMatch()), view.maxChanges(), view.changesUsed(),
                view.changesRemaining(), view.totalValue(), view.budget(), view.warnings());
    }

    @Schema(description = "Jugador de la alineación de trabajo, con su posición, estado y valor actuales.")
    record LineupPlayerResponse(
            @Schema(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
            UUID id,
            @Schema(description = "Posición actual en la plantilla.", example = "FORWARD")
            Position position,
            @Schema(description = "False si se dio de baja: hay que sustituirlo, gratis.", example = "true")
            boolean active,
            @Schema(description = "Valor de mercado vigente, en millones.", example = "12.5")
            BigDecimal value) {

        static LineupPlayerResponse from(LineupPlayerView view) {
            return new LineupPlayerResponse(view.id(), view.position(), view.active(), view.value());
        }
    }

    @Schema(description = "Partido para el que cuenta la alineación guardada ahora.")
    record TargetMatchResponse(
            @Schema(description = "Id del partido.", example = ApiExamples.MATCH_ID)
            UUID id,
            @Schema(description = "Número de jornada.", example = "3")
            int matchday,
            @Schema(description = "Hora de inicio, en UTC.", example = ApiExamples.INSTANT)
            Instant kickoffAt,
            @Schema(description = "Hora de bloqueo: lo que se guarde después cuenta para el partido siguiente.",
                    example = "2026-01-11T08:00:00Z")
            Instant lockAt) {

        /** Null without a target match. */
        static TargetMatchResponse from(TargetMatchView view) {
            return view == null ? null
                    : new TargetMatchResponse(view.id(), view.matchday(), view.kickoffAt(), view.lockAt());
        }
    }
}
