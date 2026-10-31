package com.buddyfantasy.shared.infrastructure;

import java.time.Instant;

/** The server time; hidden from the spec, like its endpoint. */
record E2eClockResponse(Instant now) {
}
