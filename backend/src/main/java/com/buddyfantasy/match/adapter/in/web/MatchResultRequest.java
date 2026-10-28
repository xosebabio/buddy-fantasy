package com.buddyfantasy.match.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Resultado final, desde el punto de vista del equipo.")
record MatchResultRequest(
        @Schema(description = "Goles a favor, de 0 a `match.limits.goals-max`.", minimum = "0", example = "3")
        @NotNull Integer goalsFor,
        @Schema(description = "Goles en contra, de 0 a `match.limits.goals-max`.", minimum = "0", example = "1")
        @NotNull Integer goalsAgainst) {
}
