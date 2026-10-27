package com.buddyfantasy.squad.application.usecases.removeplayerphoto;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/** Takes the player's photo down and deletes it (best effort). Idempotent; also allowed on retired players. */
public record RemovePlayerPhotoCommand(UUID userId, UUID leagueId, UUID playerId) implements Command<Void> {
}
