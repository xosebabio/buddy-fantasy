package com.buddyfantasy.league.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** The domain checks the length against {@code league.limits.name-max-length}. */
@Schema(description = "Liga nueva.")
record CreateLeagueRequest(
        @Schema(description = "Nombre de la liga, sin espacios al principio ni al final. Como mucho "
                + "`league.limits.name-max-length` caracteres.", example = "Peña Os Amigos")
        @NotBlank String name) {
}
