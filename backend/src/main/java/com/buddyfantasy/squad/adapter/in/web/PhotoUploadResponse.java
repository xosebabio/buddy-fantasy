package com.buddyfantasy.squad.adapter.in.web;

import java.time.Instant;
import java.util.Map;

/** The browser sends a PUT to {@code uploadUrl} with these {@code headers}, then confirms {@code photoKey}. */
record PhotoUploadResponse(String uploadUrl, Map<String, String> headers, String photoKey, Instant expiresAt) {
}
