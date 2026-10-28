package com.buddyfantasy.squad.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.usecases.confirmplayerphoto.ConfirmPlayerPhotoCommand;
import com.buddyfantasy.squad.application.usecases.getplayer.GetPlayerQuery;
import com.buddyfantasy.squad.application.usecases.getplayer.PlayerView;
import com.buddyfantasy.squad.application.usecases.removeplayerphoto.RemovePlayerPhotoCommand;
import com.buddyfantasy.squad.application.usecases.requestplayerphotoupload.RequestPlayerPhotoUploadCommand;
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
@RestController
@RequiredArgsConstructor
class PlayerPhotoController {

    private final CommandBus bus;

    @PostMapping("/api/leagues/{id}/players/{playerId}/photo/upload")
    PhotoUploadResponse requestUpload(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                      @PathVariable UUID playerId, @Valid @RequestBody PhotoUploadRequest request) {
        ImageUpload upload = bus.dispatch(new RequestPlayerPhotoUploadCommand(user.userId(), id, playerId,
                request.contentType(), request.sizeBytes()));
        return new PhotoUploadResponse(upload.uploadUrl(), upload.headers(), upload.key(), upload.expiresAt());
    }

    @PutMapping("/api/leagues/{id}/players/{playerId}/photo")
    PlayerView confirm(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID playerId,
                       @Valid @RequestBody ConfirmPhotoRequest request) {
        bus.dispatch(new ConfirmPlayerPhotoCommand(user.userId(), id, playerId, request.photoKey()));
        return bus.ask(new GetPlayerQuery(user.userId(), id, playerId));
    }

    @DeleteMapping("/api/leagues/{id}/players/{playerId}/photo")
    ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                @PathVariable UUID playerId) {
        bus.dispatch(new RemovePlayerPhotoCommand(user.userId(), id, playerId));
        return ResponseEntity.noContent().build();
    }
}
