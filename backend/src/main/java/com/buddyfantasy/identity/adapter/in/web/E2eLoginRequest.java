package com.buddyfantasy.identity.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/** The account an E2E test logs in as; hidden from the spec, like its endpoint. */
record E2eLoginRequest(@NotBlank String email, @NotBlank String displayName) {
}
