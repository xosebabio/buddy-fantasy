package com.buddyfantasy.match.adapter.in.web;

import com.buddyfantasy.match.domain.MatchResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado final, desde el punto de vista del equipo.")
record MatchResultResponse(
        @Schema(description = "Goles a favor.", example = "3")
        int goalsFor,
        @Schema(description = "Goles en contra.", example = "1")
        int goalsAgainst) {

    /** Null without a result. */
    static MatchResultResponse from(MatchResult result) {
        return result == null ? null : new MatchResultResponse(result.goalsFor(), result.goalsAgainst());
    }
}
