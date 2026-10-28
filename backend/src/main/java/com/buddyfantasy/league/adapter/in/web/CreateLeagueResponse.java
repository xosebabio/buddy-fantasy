package com.buddyfantasy.league.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liga creada, con su primer enlace de invitación.")
record CreateLeagueResponse(
        @Schema(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
        UUID id,
        @Schema(description = "Token del enlace de invitación. Solo se devuelve aquí y al regenerarlo: no se puede "
                + "volver a consultar.", example = "q7Z2mXk9LwP4vR1sT8yB3nC6dF0gH5jK")
        String invitationToken) {
}
