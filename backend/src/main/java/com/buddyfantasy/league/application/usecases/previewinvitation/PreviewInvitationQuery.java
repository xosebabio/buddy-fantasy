package com.buddyfantasy.league.application.usecases.previewinvitation;

import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record PreviewInvitationQuery(UUID userId, String token) implements Query<InvitationPreview> {

    /** The token is a credential: keep it out of logs. */
    @Override
    public String toString() {
        return "PreviewInvitationQuery[userId=" + userId + "]";
    }
}
