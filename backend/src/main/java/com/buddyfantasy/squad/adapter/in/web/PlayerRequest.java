package com.buddyfantasy.squad.adapter.in.web;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code initialValue} is in millions; {@code shirtNumber} is optional. The domain checks the remaining rules,
 * including {@code squad.limits}.
 */
@Schema(description = "Ficha de un jugador del equipo real (RF-09, RF-10).")
record PlayerRequest(
        @Schema(description = "Nombre, sin espacios al principio ni al final. Como mucho "
                + "`squad.limits.name-max-length` caracteres.", example = "Iago Aspas")
        @NotBlank String name,
        @Schema(description = "Dorsal, opcional. Entre `squad.limits.shirt-number-min` y "
                + "`squad.limits.shirt-number-max`, y sin repetir entre los jugadores activos de la liga.",
                nullable = true, example = "10")
        Integer shirtNumber,
        @Schema(description = "Posición en el campo.", example = "FORWARD")
        @NotNull Position position,
        @Schema(description = "Valor inicial en millones. Mayor que 0, con hasta 8 cifras enteras y 2 decimales.",
                exclusiveMinimum = true, minimum = "0", example = "12.5")
        @NotNull BigDecimal initialValue) {
}
