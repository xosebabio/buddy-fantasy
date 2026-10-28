package com.buddyfantasy.league.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liga a la que se ha unido el usuario.")
record JoinLeagueResponse(
        @Schema(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
        UUID leagueId) {
}
