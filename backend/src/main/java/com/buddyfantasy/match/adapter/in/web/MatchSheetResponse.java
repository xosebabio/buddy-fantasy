package com.buddyfantasy.match.adapter.in.web;

import java.util.List;

import com.buddyfantasy.match.domain.MatchSheetWarning;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/** Warning codes ({@code GOALS_MISMATCH}…) that the frontend translates; they are not errors. */
@Schema(description = "Acta guardada, con sus avisos de coherencia.")
record MatchSheetResponse(
        @ArraySchema(arraySchema = @Schema(description = """
                Avisos que la interfaz traduce; nunca impiden guardar ni cerrar, porque en una liga de peñas puede \
                faltar algún goleador. `GOALS_MISMATCH`: los goles (normales y de penalti) de los jugadores no suman \
                los goles a favor. `OWN_GOALS_EXCEED_GOALS_AGAINST`: hay más goles en propia puerta que goles en \
                contra.""", example = "[\"GOALS_MISMATCH\"]"))
        List<MatchSheetWarning> warnings) {
}
