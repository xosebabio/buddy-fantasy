package com.buddyfantasy.scoring.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.scoring.application.usecases.getmanagerhistory.GetManagerHistoryQuery;
import com.buddyfantasy.scoring.application.usecases.getplayerpoints.GetPlayerPointsQuery;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The points history of a manager (RF-33) and of a player (RF-11), for every member. Read only. */
@Tag(name = "Clasificación")
@RestController
@RequiredArgsConstructor
class ScoringHistoryController {

    private final CommandBus bus;

    @Operation(summary = "Ver el historial de un manager",
            description = """
                    Rol mínimo: miembro. Los partidos puntuados de un manager, del más antiguo al más reciente, con \
                    los puntos de cada jugador de su alineación (RF-33). Incluye los partidos de antes de su última \
                    alta si tenía alineación, marcados como fuera de la clasificación.""")
    @ApiResponse(responseCode = "200", description = "El historial del manager.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El manager no es miembro de la liga.")
    @GetMapping("/api/leagues/{id}/managers/{userId}/history")
    List<ManagerMatchdayResponse> manager(@AuthenticationPrincipal CurrentUser user,
                                          @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                          @PathVariable UUID id,
                                          @Parameter(description = "Id del manager.", example = ApiExamples.USER_ID)
                                          @PathVariable("userId") UUID managerId) {
        return bus.ask(new GetManagerHistoryQuery(user.userId(), id, managerId)).stream()
                .map(ManagerMatchdayResponse::from)
                .toList();
    }

    @Operation(summary = "Ver los puntos de un jugador",
            description = "Rol mínimo: miembro. Los partidos puntuados que jugó, del más antiguo al más reciente, con "
                    + "sus puntos por acción, por valoración y en total (RF-11). Vacía si aún no ha puntuado.")
    @ApiResponse(responseCode = "200", description = "Los puntos del jugador por partido.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @GetMapping("/api/leagues/{id}/players/{playerId}/points")
    List<PlayerMatchdayResponse> player(@AuthenticationPrincipal CurrentUser user,
                                        @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                        @PathVariable UUID id,
                                        @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                                        @PathVariable UUID playerId) {
        return bus.ask(new GetPlayerPointsQuery(user.userId(), id, playerId)).stream()
                .map(PlayerMatchdayResponse::from)
                .toList();
    }
}
