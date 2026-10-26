package com.buddyfantasy.identity.application.usecases.registerorloginuser;

import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.Command;
import jakarta.validation.constraints.NotBlank;

/** Idempotent: creates the user on first login, otherwise records the login (RF-01). */
public record RegisterOrLoginUser(
        @NotBlank String googleSub,
        @NotBlank String email,
        @NotBlank String name,
        String avatarUrl) implements Command<UserId> {
}
