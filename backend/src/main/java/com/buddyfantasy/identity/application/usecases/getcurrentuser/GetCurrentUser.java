package com.buddyfantasy.identity.application.usecases.getcurrentuser;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetCurrentUser(UUID userId) implements Query<UserView> {
}
