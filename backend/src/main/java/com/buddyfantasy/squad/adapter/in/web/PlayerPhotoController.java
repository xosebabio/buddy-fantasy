package com.buddyfantasy.squad.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.shared.web.ApiExamples;
import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.usecases.confirmplayerphoto.ConfirmPlayerPhotoCommand;
import com.buddyfantasy.squad.application.usecases.getplayer.GetPlayerQuery;
import com.buddyfantasy.squad.application.usecases.removeplayerphoto.RemovePlayerPhotoCommand;
import com.buddyfantasy.squad.application.usecases.requestplayerphotoupload.RequestPlayerPhotoUploadCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Player photo in three steps: request a presigned URL, upload straight to storage, confirm the key. */
@Tag(name = "Plantilla")
@RestController
@RequiredArgsConstructor
class PlayerPhotoController {

    private static final String RETIRED_PLAYER = """
            {"type": "about:blank", "title": "Conflict", "status": 409,
             "detail": "A retired player cannot be edited", "code": "PLAYER_RETIRED",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players/c2e4f6a8-1b3d-4e5f-8a7b-6c5d4e3f2a1b/photo"}""";

    private final CommandBus bus;

    @Operation(summary = "Pedir una URL para subir la foto de un jugador",
            description = """
                    Rol mínimo: administrador. Primer paso de los tres para cambiar la foto: el navegador reduce la \
                    imagen a WebP, pide aquí una URL prefirmada, la sube directamente al almacenamiento y la \
                    confirma con `PUT .../photo`. Hasta confirmarla, el jugador conserva su foto.""")
    @ApiResponse(responseCode = "200", description = "La URL y las cabeceras de la subida.")
    @ApiResponse(responseCode = "400", description = "Tipo distinto de `image/webp` (`code`: `INVALID_PHOTO_FORMAT`) "
            + "o tamaño fuera de 1 a `squad.limits.photo-max-size` (`INVALID_PHOTO_SIZE`).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "Player photos must be image/webp", "code": "INVALID_PHOTO_FORMAT",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players/c2e4f6a8-1b3d-4e5f-8a7b-6c5d4e3f2a1b/photo/upload"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @ApiResponse(responseCode = "409", description = "El jugador está dado de baja (`code`: `PLAYER_RETIRED`).",
            content = @Content(examples = @ExampleObject(value = RETIRED_PLAYER)))
    @PostMapping("/api/leagues/{id}/players/{playerId}/photo/upload")
    PhotoUploadResponse requestUpload(@AuthenticationPrincipal CurrentUser user,
                                      @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                      @PathVariable UUID id,
                                      @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                                      @PathVariable UUID playerId,
                                      @Valid @RequestBody PhotoUploadRequest request) {
        ImageUpload upload = bus.dispatch(new RequestPlayerPhotoUploadCommand(user.userId(), id, playerId,
                request.contentType(), request.sizeBytes()));
        return new PhotoUploadResponse(upload.uploadUrl(), upload.headers(), upload.key(), upload.expiresAt());
    }

    @Operation(summary = "Confirmar la foto subida",
            description = """
                    Rol mínimo: administrador. Último paso: comprueba que la imagen subida existe y cumple los \
                    límites, la pone como foto del jugador y borra la anterior. Confirmar la foto que ya tiene no \
                    hace nada.""")
    @ApiResponse(responseCode = "200", description = "El jugador con su foto nueva.")
    @ApiResponse(responseCode = "400", description = "No se subió nada con la clave (`code`: `PHOTO_NOT_UPLOADED`), "
            + "la imagen subida no cumple los límites (`INVALID_PHOTO_FORMAT` o `INVALID_PHOTO_SIZE`), o la clave "
            + "no es de este jugador (sin `code`: no la pidió la interfaz).",
            content = @Content(examples = @ExampleObject(value = """
                    {"type": "about:blank", "title": "Bad Request", "status": 400,
                     "detail": "No photo was uploaded under that key", "code": "PHOTO_NOT_UPLOADED",
                     "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players/c2e4f6a8-1b3d-4e5f-8a7b-6c5d4e3f2a1b/photo"}""")))
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @ApiResponse(responseCode = "409", description = "El jugador está dado de baja (`code`: `PLAYER_RETIRED`).",
            content = @Content(examples = @ExampleObject(value = RETIRED_PLAYER)))
    @PutMapping("/api/leagues/{id}/players/{playerId}/photo")
    PlayerResponse confirm(@AuthenticationPrincipal CurrentUser user,
                           @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                           @PathVariable UUID id,
                           @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                           @PathVariable UUID playerId,
                           @Valid @RequestBody ConfirmPhotoRequest request) {
        bus.dispatch(new ConfirmPlayerPhotoCommand(user.userId(), id, playerId, request.photoKey()));
        return PlayerResponse.from(bus.ask(new GetPlayerQuery(user.userId(), id, playerId)));
    }

    @Operation(summary = "Quitar la foto de un jugador",
            description = "Rol mínimo: administrador. Borra la foto del jugador, también si está dado de baja. Sin "
                    + "foto no hace nada.")
    @ApiResponse(responseCode = "204", description = "El jugador ya no tiene foto.")
    @ApiResponse(responseCode = "403", description = "El usuario no es el administrador de la liga.")
    @ApiResponse(responseCode = "404", description = "El jugador no es de la liga.")
    @DeleteMapping("/api/leagues/{id}/players/{playerId}/photo")
    ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user,
                                @Parameter(description = "Id de la liga.", example = ApiExamples.LEAGUE_ID)
                                @PathVariable UUID id,
                                @Parameter(description = "Id del jugador.", example = ApiExamples.PLAYER_ID)
                                @PathVariable UUID playerId) {
        bus.dispatch(new RemovePlayerPhotoCommand(user.userId(), id, playerId));
        return ResponseEntity.noContent().build();
    }
}
