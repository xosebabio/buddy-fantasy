package com.buddyfantasy.league.application.usecases.joinleague;

import java.time.Clock;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Invitation;
import com.buddyfantasy.league.domain.LeagueId;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

@Component
class JoinLeagueHandler implements CommandHandler<JoinLeagueCommand, LeagueId> {

    private final InvitationRepository invitations;
    private final MembershipRepository memberships;
    private final Clock clock;

    JoinLeagueHandler(InvitationRepository invitations, MembershipRepository memberships, Clock clock) {
        this.invitations = invitations;
        this.memberships = memberships;
        this.clock = clock;
    }

    @Override
    public LeagueId handle(JoinLeagueCommand command) {
        Invitation invitation = invitations.findActiveByTokenHash(Invitation.hash(command.token()))
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        LeagueId leagueId = invitation.leagueId();
        if (memberships.findByLeagueIdAndUserId(leagueId.value(), command.userId()).isEmpty()) {
            memberships.save(Membership.join(leagueId, command.userId(), LeagueRole.MANAGER, clock.instant()));
        }
        return leagueId;
    }
}
