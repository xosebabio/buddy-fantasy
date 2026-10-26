package com.buddyfantasy.identity;

import java.util.UUID;

/** Public profile data of a user, safe to show to other users (no email). */
public record UserSummary(UUID id, String displayName, String avatarUrl) {
}
