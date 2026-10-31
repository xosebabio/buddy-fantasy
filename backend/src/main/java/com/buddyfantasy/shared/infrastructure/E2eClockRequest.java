package com.buddyfantasy.shared.infrastructure;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

/** How much to move the E2E clock forward, as an ISO-8601 duration ({@code PT26H}); hidden from the spec. */
record E2eClockRequest(@NotNull Duration advance) {
}
