package com.buddyfantasy.squad.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import com.buddyfantasy.squad.application.usecases.getplayer.GetPlayerQuery;
import com.buddyfantasy.squad.application.usecases.listplayers.ListPlayersQuery;
import com.buddyfantasy.squad.application.usecases.registerplayer.RegisterPlayerCommand;
import com.buddyfantasy.squad.application.usecases.retireplayer.RetirePlayerCommand;
import com.buddyfantasy.squad.application.usecases.updateplayer.UpdatePlayerCommand;
import com.buddyfantasy.squad.domain.PlayerId;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plantilla", description = "Los jugadores del equipo real de la liga y su ficha (RF-09 a RF-11).")
@RestController
@RequiredArgsConstructor
class PlayersController {

    private static final String SHIRT_NUMBER_TAKEN = """
            {"type": "about:blank", "title": "Conflict", "status": 409,
             "detail": "Shirt number already taken by an active player",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players"}""";
    private static final String INVALID_PLAYER = """
            {"type": "about:blank", "title": "Bad Request", "status": 400,
             "detail": "Shirt number must be 0-99",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players"}""";

    private final CommandBus bus;

    @Operation(summary = "Dar de alta un jugador",
            description = "Rol mínimo: administrador. Alta en la plantilla con su valor inicial (RF-09).")
    @ApiResponse(responseCode = "201", description = "Jugador dado de alta. La cabecera `Location` apunta a él.",
            headers = @Header(name = "Location", description = "Ruta del jugador creado.",
                    schema = @Schema(type = "string",
                            example = "/api/leagues/" + ApiExamples.LEAGUE_ID + "/players/" + ApiExamples.PLAYER_ID)))
    @ApiResponse(responseCode = "400", description = "Nombre, dorsal o valor fuera de sus límites (`squad.limits`).",
            content = @Content(examples = @ExampleObject(value = INVALID_PLAYER)))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "409", description = "Otro jugador activo ya tiene ese dorsal.",
            content = @Content(examples = @ExampleObject(value = SHIRT_NUMBER_TAKEN)))
    @PostMapping("/api/leagues/{id}/players")
    ResponseEntity<PlayerResponse> register(@AuthenticationPrincipal CurrentUser user,
                                            @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                            @PathVariable UUID id,
                                            @Valid @RequestBody PlayerRequest request) {
        PlayerId playerId = bus.dispatch(new RegisterPlayerCommand(user.userId(), id, request.name(),
                request.shirtNumber(), request.position(), request.initialValue()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id + "/players/" + playerId.value()))
                .body(get(user, id, playerId.value()));
    }

    @Operation(summary = "Ver la plantilla",
            description = "Rol mínimo: miembro. Porteros primero y después por dorsal (sin dorsal al final) y nombre.")
    @ApiResponse(responseCode = "200", description = "Los jugadores de la liga.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @GetMapping("/api/leagues/{id}/players")
    List<PlayerResponse> list(@AuthenticationPrincipal CurrentUser user,
                              @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                              @PathVariable UUID id,
                              @Parameter(description = "Incluir también los jugadores dados de baja. Solo vale para "
                                      + "el administrador: a un manager se le ignora.", example = "false")
                              @RequestParam(defaultValue = "false") boolean includeRetired) {
        return bus.ask(new ListPlayersQuery(user.userId(), id, includeRetired)).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    @Operation(summary = "Ver un jugador",
            description = "Rol mínimo: miembro. También los dados de baja.")
    @ApiResponse(responseCode = "200", description = "El jugador.")
    @ApiResponse(responseCode = "403", description = "El usuario no es miembro de la liga (o no existe).")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @GetMapping("/api/leagues/{id}/players/{playerId}")
    PlayerResponse get(@AuthenticationPrincipal CurrentUser user,
                       @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                       @PathVariable UUID id,
                       @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                       @PathVariable UUID playerId) {
        return PlayerResponse.from(bus.ask(new GetPlayerQuery(user.userId(), id, playerId)));
    }

    @Operation(summary = "Editar un jugador",
            description = "Rol mínimo: administrador. Reemplaza nombre, dorsal, posición y valor inicial (RF-10). "
                    + "Un jugador dado de baja no se puede editar.")
    @ApiResponse(responseCode = "200", description = "El jugador editado.")
    @ApiResponse(responseCode = "400", description = "Nombre, dorsal o valor fuera de sus límites (`squad.limits`).",
            content = @Content(examples = @ExampleObject(value = INVALID_PLAYER)))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @ApiResponse(responseCode = "409", description = "Otro jugador activo ya tiene ese dorsal, o el jugador está "
            + "dado de baja.", content = @Content(examples = @ExampleObject(value = SHIRT_NUMBER_TAKEN)))
    @PutMapping("/api/leagues/{id}/players/{playerId}")
    PlayerResponse update(@AuthenticationPrincipal CurrentUser user,
                          @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                          @PathVariable UUID id,
                          @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                          @PathVariable UUID playerId,
                          @Valid @RequestBody PlayerRequest request) {
        bus.dispatch(new UpdatePlayerCommand(user.userId(), id, playerId, request.name(), request.shirtNumber(),
                request.position(), request.initialValue()));
        return get(user, id, playerId);
    }

    @Operation(summary = "Dar de baja un jugador",
            description = """
                    Rol mínimo: administrador. Baja lógica: el jugador sigue en las actas, puntos e historiales, pero \
                    ya no se puede alinear ni añadir a un acta, y sustituirlo en una alineación es gratis (RF-10). \
                    Repetirla no hace nada.""")
    @ApiResponse(responseCode = "204", description = "Jugador dado de baja.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @DeleteMapping("/api/leagues/{id}/players/{playerId}")
    ResponseEntity<Void> retire(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id,
                                @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                                @PathVariable UUID playerId) {
        bus.dispatch(new RetirePlayerCommand(user.userId(), id, playerId));
        return ResponseEntity.noContent().build();
    }
}
