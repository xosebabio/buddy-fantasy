package com.buddyfantasy.shared.web;

/**
 * Values shared by the OpenAPI examples of every module, so the examples of one Sunday league fit together. Constants
 * because annotation attributes need them.
 */
public final class ApiExamples {

    public static final String LEAGUE_ID = "3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e";
    public static final String USER_ID = "7a1d9e3b-2c4f-4b6a-8d5e-9f0a1b2c3d4e";
    public static final String OTHER_USER_ID = "e4b7c1d9-6a2f-4e8b-b3c5-0d1e2f3a4b5c";
    public static final String PLAYER_ID = "c2e4f6a8-1b3d-4e5f-8a7b-6c5d4e3f2a1b";
    public static final String OTHER_PLAYER_ID = "9d1f3b5e-7a2c-4d6e-a8b0-2c4e6f8a0b1d";
    public static final String MATCH_ID = "5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f";
    public static final String INSTANT = "2026-01-11T10:00:00Z";
    public static final String LATER_INSTANT = "2026-01-12T12:30:00Z";

    /** The problem detail of an error, as {@code ApiExceptionHandler} writes it. */
    public static final String BAD_REQUEST = """
            {"type": "about:blank", "title": "Bad Request", "status": 400, "detail": "Invalid request content.",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/players"}""";
    public static final String FORBIDDEN = """
            {"type": "about:blank", "title": "Forbidden", "status": 403, "detail": "Forbidden",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/settings"}""";
    public static final String NOT_FOUND = """
            {"type": "about:blank", "title": "Not Found", "status": 404, "detail": "Not found",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches/5b8d0f2a-4c6e-4a1b-9d3f-7e9a1c3b5d7f"}""";
    public static final String CONFLICT = """
            {"type": "about:blank", "title": "Conflict", "status": 409, "detail": "Matchday already has a match",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/matches"}""";
    public static final String INTERNAL_ERROR = """
            {"type": "about:blank", "title": "Internal Server Error", "status": 500, "detail": "Internal error",
             "instance": "/api/leagues/3f6c2a1e-8b4d-4f5a-9c7e-1d2b3a4c5d6e/standings"}""";

    private ApiExamples() {
    }
}
