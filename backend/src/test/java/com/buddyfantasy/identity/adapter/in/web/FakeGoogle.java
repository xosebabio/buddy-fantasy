package com.buddyfantasy.identity.adapter.in.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Google's token, key and user info endpoints, so a test goes through the real OAuth2 login: the browser part (the
 * consent screen) is skipped by calling the callback with a code, and the backend exchanges it here for an ID token
 * signed with a test key. One per JVM; {@link #register} points the {@code google} provider at it.
 */
final class FakeGoogle {

    static final String ISSUER = "https://accounts.google.com";
    static final String CLIENT_ID = "test-client";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final RSAKey KEY = generateKey();
    private static final HttpServer SERVER = start();

    /** The account that logs in next, and the nonce of its authorization request. */
    private static volatile Account account;
    private static volatile String nonce;

    record Account(String sub, String email, String name, boolean emailVerified) {
    }

    private FakeGoogle() {
    }

    static void register(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + SERVER.getAddress().getPort();
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-secret");
        registry.add("spring.security.oauth2.client.provider.google.token-uri", () -> base + "/token");
        registry.add("spring.security.oauth2.client.provider.google.jwk-set-uri", () -> base + "/jwks");
        registry.add("spring.security.oauth2.client.provider.google.user-info-uri", () -> base + "/userinfo");
    }

    /** The next code exchanged logs {@code next} in, for the authorization request that sent {@code requestNonce}. */
    static void nextLogin(Account next, String requestNonce) {
        account = next;
        nonce = requestNonce;
    }

    private static HttpServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/token", exchange -> respond(exchange, Map.of(
                    "access_token", "test-access-token",
                    "token_type", "Bearer",
                    "expires_in", 3600,
                    "scope", "openid profile email",
                    "id_token", idToken())));
            server.createContext("/jwks", exchange -> respond(exchange, new JWKSet(KEY.toPublicJWK()).toJSONObject()));
            server.createContext("/userinfo", exchange -> respond(exchange, Map.of(
                    "sub", account.sub(),
                    "email", account.email(),
                    "email_verified", account.emailVerified(),
                    "name", account.name(),
                    "picture", "https://pics.example/" + account.sub())));
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String idToken() {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(account.sub())
                .audience(List.of(CLIENT_ID))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("nonce", nonce)
                .claim("email", account.email())
                .claim("email_verified", account.emailVerified())
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(),
                    claims);
            jwt.sign(new RSASSASigner(KEY));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
