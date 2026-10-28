package com.buddyfantasy.rating.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * All the member's votes in the match, in any order; the players missing lose the member's vote. The domain checks
 * the window, the players and the format.
 */
@Schema(description = "Todas las valoraciones del miembro en el partido (RF-18, RF-20).")
record RatingsRequest(
        @ArraySchema(arraySchema = @Schema(description = """
                Sus votos, en cualquier orden y como mucho uno por jugador. Reemplazan los anteriores: un jugador que \
                no aparece se queda sin su voto. Vacía para retirarlos todos."""))
        @NotNull List<@NotNull @Valid Rating> ratings) {

    /** A whole value: a decimal one is rejected when reading the body. */
    @Schema(name = "PlayerRatingRequest", description = "Voto a un jugador.")
    record Rating(
            @Schema(description = "Id de un jugador del acta.", example = ApiExamples.PLAYER_ID)
            @NotNull UUID playerId,
            @Schema(description = "Valor entero en el formato de la ventana: de 0 a 4 picas (`SPADES`) o de 0 a 10 "
                    + "(`DECIMAL`). Un decimal da `400`.", minimum = "0", maximum = "10", example = "3")
            @NotNull Integer value) {
    }
}
