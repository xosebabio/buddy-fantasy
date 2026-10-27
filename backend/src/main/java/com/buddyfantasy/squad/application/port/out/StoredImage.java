package com.buddyfantasy.squad.application.port.out;

/** What the storage reports about an uploaded image, used to check it against what was declared. */
public record StoredImage(String contentType, long sizeBytes) {
}
