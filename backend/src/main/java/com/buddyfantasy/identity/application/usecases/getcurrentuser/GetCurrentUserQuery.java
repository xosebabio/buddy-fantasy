package com.buddyfantasy.identity.application.usecases.getcurrentuser;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record GetCurrentUserQuery(UUID userId) implements Query<UserView> {
}
