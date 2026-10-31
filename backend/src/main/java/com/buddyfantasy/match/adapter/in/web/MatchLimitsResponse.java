package com.buddyfantasy.match.adapter.in.web;

import com.buddyfantasy.match.application.usecases.getmatchlimits.MatchLimitsView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Límites configurados de los partidos y el acta (`match.limits`), para validar los "
        + "formularios antes de enviarlos. Son globales, no cambian por liga.")
record MatchLimitsResponse(
        @Schema(description = "Caracteres máximos del rival (`match.limits.opponent-max-length`).", example = "100")
        int opponentMaxLength,
        @Schema(description = "Jornada mínima (`match.limits.matchday-min`).", example = "1")
        int matchdayMin,
        @Schema(description = "Goles máximos por equipo en el resultado (`match.limits.goals-max`).", example = "99")
        int goalsMax,
        @Schema(description = "Máximo por jugador y estadística del acta (`match.limits.stat-count-max`).",
                example = "99")
        int statCountMax,
        @Schema(description = "Jugadores máximos en el acta (`match.limits.sheet-players-max`).", example = "30")
        int sheetPlayersMax) {

    static MatchLimitsResponse from(MatchLimitsView view) {
        return new MatchLimitsResponse(view.opponentMaxLength(), view.matchdayMin(), view.goalsMax(),
                view.statCountMax(), view.sheetPlayersMax());
    }
}
