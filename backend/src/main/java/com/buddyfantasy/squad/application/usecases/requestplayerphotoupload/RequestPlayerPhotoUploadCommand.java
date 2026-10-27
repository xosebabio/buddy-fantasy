package com.buddyfantasy.squad.application.usecases.requestplayerphotoupload;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;
import com.buddyfantasy.squad.application.port.out.ImageUpload;

/**
 * RF-09: presigned URL for the browser to upload a player photo directly to storage. Does not touch the database;
 * the photo is only set once it is confirmed.
 */
public record RequestPlayerPhotoUploadCommand(UUID userId, UUID leagueId, UUID playerId, String contentType,
                                              long sizeBytes) implements Command<ImageUpload> {
}
