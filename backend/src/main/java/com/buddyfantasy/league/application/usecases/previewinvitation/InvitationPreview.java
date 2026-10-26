package com.buddyfantasy.league.application.usecases.previewinvitation;

/** What the "join league X?" screen may know: the league name and whether the caller is already a member. */
public record InvitationPreview(String leagueName, boolean alreadyMember) {
}
