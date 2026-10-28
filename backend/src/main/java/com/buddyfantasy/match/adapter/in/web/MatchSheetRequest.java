package com.buddyfantasy.match.adapter.in.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * {@code result} is null in a draft of a scheduled match. The domain checks the remaining rules, including
 * {@code match.limits}.
 */
@Schema(description = "Acta completa del partido (RF-13, RF-14): reemplaza la anterior.")
record MatchSheetRequest(
        @Schema(description = "Resultado; null en un borrador de un partido sin cerrar. Un partido cerrado lo "
                + "necesita.", nullable = true)
        @Valid MatchResultRequest result,
        @ArraySchema(arraySchema = @Schema(description = "Los jugadores que jugaron, cada uno una vez y como mucho "
                + "`match.limits.sheet-players-max`. Un partido cerrado necesita al menos uno."))
        @NotNull List<@NotNull @Valid PlayerPerformanceRequest> performances) {
}
