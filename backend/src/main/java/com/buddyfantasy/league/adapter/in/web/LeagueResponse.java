package com.buddyfantasy.league.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.usecases.getleague.LeagueView;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Liga vista por uno de sus miembros, con el rol que tiene en ella.")
record LeagueResponse(
        @Schema(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
        UUID id,
        @Schema(description = "Nombre de la liga.", example = "Peña Os Amigos")
        String name,
        @Schema(description = "Cuándo se creó.", example = ApiExamples.INSTANT)
        Instant createdAt,
        @Schema(description = "Rol del usuario en la liga. `ADMIN` también juega como manager.", example = "MANAGER")
        LeagueRole role) {

    static LeagueResponse from(LeagueView view) {
        return new LeagueResponse(view.id(), view.name(), view.createdAt(), view.role());
    }
}
