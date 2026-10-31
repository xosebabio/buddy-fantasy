package com.buddyfantasy.squad.adapter.in.web;

import com.buddyfantasy.squad.application.usecases.getsquadlimits.SquadLimitsView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Límites configurados de la ficha de un jugador (`squad.limits`), para validar el "
        + "formulario antes de enviarlo. Son globales, no cambian por liga.")
record SquadLimitsResponse(
        @Schema(description = "Caracteres máximos del nombre (`squad.limits.name-max-length`).", example = "100")
        int nameMaxLength,
        @Schema(description = "Dorsal mínimo (`squad.limits.shirt-number-min`).", example = "0")
        int shirtNumberMin,
        @Schema(description = "Dorsal máximo (`squad.limits.shirt-number-max`).", example = "99")
        int shirtNumberMax) {

    static SquadLimitsResponse from(SquadLimitsView view) {
        return new SquadLimitsResponse(view.nameMaxLength(), view.shirtNumberMin(), view.shirtNumberMax());
    }
}
