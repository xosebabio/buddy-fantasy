package com.buddyfantasy.squad.adapter.in.web;

import java.math.BigDecimal;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.squad.domain.Player;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code initialValue} is in millions; {@code shirtNumber} is optional. The domain checks the remaining rules. */
record PlayerRequest(@NotBlank @Size(max = Player.MAX_NAME_LENGTH) String name,
                     Integer shirtNumber,
                     @NotNull Position position,
                     @NotNull BigDecimal initialValue) {
}
