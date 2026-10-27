package com.buddyfantasy.squad.adapter.in.web;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code initialValue} is in millions; {@code shirtNumber} is optional. The domain checks the remaining rules,
 * including {@code squad.limits}.
 */
record PlayerRequest(@NotBlank String name,
                     Integer shirtNumber,
                     @NotNull Position position,
                     @NotNull BigDecimal initialValue) {
}
