package com.buddyfantasy.squad.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** What the browser is about to upload; the domain checks the type and size. */
record PhotoUploadRequest(@NotBlank String contentType, @NotNull Long sizeBytes) {
}
