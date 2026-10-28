package com.buddyfantasy.match.adapter.in.web;

import java.time.Instant;

import com.buddyfantasy.match.domain.Venue;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code kickoffAt} is an ISO-8601 instant in UTC. The domain checks the remaining rules, including
 * {@code match.limits}.
 */
@Schema(description = "Datos de un partido del calendario (RF-12). Una jornada tiene un único partido.")
record MatchRequest(
        @Schema(description = "Número de jornada, desde `match.limits.matchday-min` y sin repetir en la liga.",
                example = "3")
        @NotNull Integer matchday,
        @Schema(description = "Nombre del rival, sin espacios al principio ni al final. Como mucho "
                + "`match.limits.opponent-max-length` caracteres.", example = "Atlético Lavadores")
        @NotBlank String opponent,
        @Schema(description = "Si el equipo juega en casa o fuera.", example = "HOME")
        @NotNull Venue venue,
        @Schema(description = "Fecha y hora de inicio, en UTC. Dos partidos de la liga no pueden empezar a la vez.",
                example = ApiExamples.INSTANT)
        @NotNull Instant kickoffAt) {
}
