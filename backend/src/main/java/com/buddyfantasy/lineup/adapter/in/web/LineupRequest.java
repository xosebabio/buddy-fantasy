package com.buddyfantasy.lineup.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The whole lineup, in any order. The domain checks size, players, formation, budget and changes. */
@Schema(description = "Alineación completa del manager (RF-24).")
record LineupRequest(
        @ArraySchema(arraySchema = @Schema(description = """
                Ids de los jugadores, en cualquier orden: exactamente `lineupSize` jugadores activos de la plantilla, \
                sin repetir, con los mínimos por posición si la liga exige formación y dentro del presupuesto si lo \
                tiene."""),
                schema = @Schema(example = ApiExamples.PLAYER_ID))
        @NotNull List<@NotNull UUID> playerIds) {
}
