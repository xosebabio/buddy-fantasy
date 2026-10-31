package com.buddyfantasy.shared.infrastructure;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE-34: the E2E tests read the server time and move it forward ({@link E2eClock}), only with the {@code e2e} profile.
 * Like any endpoint, it needs a session, and CSRF applies to the POST. The clock is the whole server's: the specs that
 * move it run one after the other.
 */
@Hidden
@Profile(E2eClock.PROFILE)
@RestController
@RequiredArgsConstructor
class E2eClockController {

    static final String PATH = "/api/e2e/clock";

    private static final Logger log = LoggerFactory.getLogger(E2eClockController.class);

    private final E2eClock clock;

    @PostConstruct
    void warn() {
        log.warn("Profile '{}' active: {} moves the server clock forward. Never use it in production.",
                E2eClock.PROFILE, PATH);
    }

    @GetMapping(PATH)
    E2eClockResponse now() {
        return new E2eClockResponse(clock.instant());
    }

    @PostMapping(PATH)
    E2eClockResponse advance(@Valid @RequestBody E2eClockRequest request) {
        return new E2eClockResponse(clock.advance(request.advance()));
    }
}
