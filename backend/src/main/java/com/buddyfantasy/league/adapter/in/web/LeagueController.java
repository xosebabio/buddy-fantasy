package com.buddyfantasy.league.adapter.in.web;

import java.net.URI;
import java.util.UUID;

import com.buddyfantasy.league.application.usecases.createleague.CreateLeagueCommand;
import com.buddyfantasy.league.application.usecases.createleague.CreatedLeague;
import com.buddyfantasy.league.application.usecases.getleague.GetLeagueQuery;
import com.buddyfantasy.league.application.usecases.getleaguesettings.GetLeagueSettingsQuery;
import com.buddyfantasy.league.application.usecases.updateleaguesettings.UpdateLeagueSettingsCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ligas", description = "Crear una liga, consultarla y cambiar sus parámetros (RF-05, RF-07).")
@RestController
@RequiredArgsConstructor
class LeagueController {

    private final CommandBus bus;

    @Operation(summary = "Crear una liga",
            description = "Cualquier usuario puede crear una liga: queda como su administrador, con los parámetros "
                    + "de `league.defaults` y un primer enlace de invitación (RF-05).")
    @ApiResponse(responseCode = "201", description = "Liga creada. La cabecera `Location` apunta a ella.",
            headers = @Header(name = "Location", description = "Ruta de la liga creada.",
                    schema = @Schema(type = "string", example = "/api/leagues/" + ApiExamples.LEAGUE_ID)))
    @ApiResponse(responseCode = "400", description = "Nombre vacío o más largo que `league.limits.name-max-length`.",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "League name must be 1-100 characters", "instance": "/api/leagues"}""")))
    @PostMapping("/api/leagues")
    ResponseEntity<CreateLeagueResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                @Valid @RequestBody CreateLeagueRequest request) {
        CreatedLeague created = bus.dispatch(new CreateLeagueCommand(user.userId(), request.name()));
        return ResponseEntity.created(URI.create("/api/leagues/" + created.id().value()))
                .body(new CreateLeagueResponse(created.id().value(), created.invitationToken()));
    }

    @Operation(summary = "Ver una liga", description = "Rol mínimo: miembro. Incluye el rol del usuario en ella.")
    @ApiResponse(responseCode = "200", description = "La liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}")
    LeagueResponse get(@AuthenticationPrincipal CurrentUser user,
                       @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                       @PathVariable UUID id) {
        return LeagueResponse.from(bus.ask(new GetLeagueQuery(user.userId(), id)));
    }

    @Operation(summary = "Ver los parámetros de la liga",
            description = "Rol mínimo: miembro. Todos los parámetros configurables y la tabla de puntos (RF-07).")
    @ApiResponse(responseCode = "200", description = "Los parámetros de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/settings")
    LeagueSettingsBody settings(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id) {
        return LeagueSettingsWebMapper.toBody(bus.ask(new GetLeagueSettingsQuery(user.userId(), id)));
    }

    @Operation(summary = "Cambiar los parámetros de la liga",
            description = """
                    Rol mínimo: administrador. Reemplaza todos los parámetros y la tabla de puntos (RF-07). Valen \
                    para lo que venga: una ventana de valoración ya abierta y una jornada ya puntuada conservan los \
                    que congelaron.""")
    @ApiResponse(responseCode = "200", description = "Los parámetros guardados.")
    @ApiResponse(responseCode = "400", description = "Algún parámetro fuera de su rango, o falta alguna acción o "
            + "posición en la tabla de puntos.", content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "lineup minimums per position cannot add up to more than lineupSize",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/settings"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @PutMapping("/api/leagues/{id}/settings")
    LeagueSettingsBody updateSettings(@AuthenticationPrincipal CurrentUser user,
                                      @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                      @PathVariable UUID id,
                                      @RequestBody LeagueSettingsBody body) {
        bus.dispatch(new UpdateLeagueSettingsCommand(user.userId(), id,
                LeagueSettingsWebMapper.toSettings(body), LeagueSettingsWebMapper.toScoringRules(body)));
        return settings(user, id);
    }
}
