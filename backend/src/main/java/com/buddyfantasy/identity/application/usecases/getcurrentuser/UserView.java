package com.buddyfantasy.identity.application.usecases.getcurrentuser;

import java.util.UUID;

public record UserView(UUID id, String displayName, String email, String avatarUrl) {
}
