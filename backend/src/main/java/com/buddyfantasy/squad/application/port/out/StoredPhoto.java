package com.buddyfantasy.squad.application.port.out;

/** What the storage reports about an uploaded photo, used to check it against what was declared. */
public record StoredPhoto(String contentType, long sizeBytes) {
}
