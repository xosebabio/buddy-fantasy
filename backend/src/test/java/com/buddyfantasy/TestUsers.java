package com.buddyfantasy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.buddyfantasy.shared.security.CurrentUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

/**
 * Authenticated MockMvc requests: {@code .with(as(userId))} logs the request in as that user, as Google login does.
 * Not a Lombok {@code @UtilityClass}, which cannot be statically imported.
 */
public final class TestUsers {

    private static final Instant ISSUED_AT = Instant.parse("2026-01-10T10:00:00Z");

    private TestUsers() {
    }

    public static RequestPostProcessor as(UUID userId) {
        OidcIdToken idToken = new OidcIdToken("t", ISSUED_AT, ISSUED_AT.plusSeconds(3600), Map.of("sub", "any"));
        return oidcLogin().oidcUser(new CurrentUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken, null, userId));
    }
}
