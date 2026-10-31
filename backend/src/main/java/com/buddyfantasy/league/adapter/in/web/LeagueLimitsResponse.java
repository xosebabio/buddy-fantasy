package com.buddyfantasy.league.adapter.in.web;

import com.buddyfantasy.league.application.usecases.getleaguelimits.LeagueLimitsView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Límites configurados de los parámetros de la liga (`league.limits`), para validar el "
        + "formulario antes de enviarlo. Son globales, no cambian por liga.")
record LeagueLimitsResponse(
        @Schema(description = "Horas máximas de antelación del bloqueo de alineaciones "
                + "(`league.limits.lineup-lock-hours-max`).", example = "168")
        int lineupLockHoursMax) {

    static LeagueLimitsResponse from(LeagueLimitsView view) {
        return new LeagueLimitsResponse(view.lineupLockHoursMax());
    }
}
