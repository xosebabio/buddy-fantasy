package com.buddyfantasy.league.application.usecases.previewinvitation;

import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Invitation;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.shared.domain.UnexpectedException;
import org.springframework.stereotype.Component;

@Component
class PreviewInvitationHandler implements QueryHandler<PreviewInvitationQuery, InvitationPreview> {

    private final InvitationRepository invitations;
    private final LeagueRepository leagues;
    private final MembershipRepository memberships;

    PreviewInvitationHandler(InvitationRepository invitations, LeagueRepository leagues,
                             MembershipRepository memberships) {
        this.invitations = invitations;
        this.leagues = leagues;
        this.memberships = memberships;
    }

    @Override
    public InvitationPreview handle(PreviewInvitationQuery query) {
        Invitation invitation = invitations.findActiveByTokenHash(Invitation.hash(query.token()))
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        String leagueName = leagues.findById(invitation.leagueId().value())
                .orElseThrow(() -> new UnexpectedException("League not found: " + invitation.leagueId().value()))
                .name();
        boolean member = memberships.findByLeagueIdAndUserId(invitation.leagueId().value(), query.userId())
                .isPresent();
        return new InvitationPreview(leagueName, member);
    }
}
