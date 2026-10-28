package com.buddyfantasy.league.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enlace de invitación nuevo.")
record InvitationTokenResponse(
        @Schema(description = "Token del enlace. Solo se devuelve aquí: no se puede volver a consultar.",
                example = "q7Z2mXk9LwP4vR1sT8yB3nC6dF0gH5jK")
        String token) {
}
