package com.buddyfantasy.squad.application.usecases.confirmplayerphoto;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * RF-09: sets the photo the browser has just uploaded. The previous photo is deleted (best effort). Idempotent with
 * the same key.
 */
public record ConfirmPlayerPhotoCommand(UUID userId, UUID leagueId, UUID playerId, String photoKey)
        implements Command<Void> {
}
