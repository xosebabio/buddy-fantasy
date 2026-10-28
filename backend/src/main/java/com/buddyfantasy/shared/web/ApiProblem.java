package com.buddyfantasy.shared.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documentation-only schema of the {@code ProblemDetail} that {@code ApiExceptionHandler} answers with: the OpenAPI
 * customizer attaches it to every error response. Never instantiated.
 */
@Schema(name = "Problem", description = "Error en formato RFC 9457 (`application/problem+json`).")
public record ApiProblem(
        @Schema(description = "Tipo del error; siempre `about:blank`.", example = "about:blank")
        String type,
        @Schema(description = "Texto estándar del código HTTP.", example = "Conflict")
        String title,
        @Schema(description = "Código HTTP.", example = "409")
        int status,
        @Schema(description = "Motivo legible, en inglés y solo para depurar: la interfaz no debe mostrarlo.",
                example = "The lineup has 3 changes and at most 2 are allowed per matchday")
        String detail,
        @Schema(description = "Ruta de la petición.",
                example = "/api/leagues/" + ApiExamples.LEAGUE_ID + "/lineup")
        String instance,
        @Schema(description = "Motivo para la interfaz cuando debe distinguirlo; cada endpoint documenta los suyos.",
                nullable = true, example = "MAX_CHANGES_EXCEEDED")
        String code,
        @ArraySchema(arraySchema = @Schema(description = "Restricciones incumplidas, solo en algunos `400` de "
                + "validación.", nullable = true, example = "[\"name: must not be blank\"]"))
        List<String> violations) {
}
