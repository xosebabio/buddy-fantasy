package com.buddyfantasy.identity.domain;

import java.time.Instant;

import com.buddyfantasy.shared.domain.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Account identified by its Google {@code sub}; the email is informative and may change. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class User extends AggregateRoot {

    private final UserId id;
    private final String googleSub;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final Instant createdAt;
    private Instant lastLoginAt;

    public static User register(String googleSub, String email, String displayName, String avatarUrl, Instant now) {
        return new User(UserId.generate(), googleSub, email, displayName, avatarUrl, now, now);
    }

    /** Later logins only touch {@code lastLoginAt}, so profile edits are never overwritten. */
    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
    }
}
