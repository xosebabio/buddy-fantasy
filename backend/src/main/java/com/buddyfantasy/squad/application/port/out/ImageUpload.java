package com.buddyfantasy.squad.application.port.out;

import java.time.Instant;
import java.util.Map;

/** A presigned upload. The client must send a PUT to {@code uploadUrl} with exactly these {@code headers}. */
public record ImageUpload(String uploadUrl, Map<String, String> headers, String key, Instant expiresAt) {
}
