package com.buddyfantasy.league.application.usecases.regenerateinvitation;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.league.domain.Invitation;
import com.buddyfantasy.league.domain.LeagueId;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class RegenerateInvitationHandler implements CommandHandler<RegenerateInvitationCommand, String> {

    private final LeagueAccess access;
    private final InvitationRepository invitations;
    private final Clock clock;

    RegenerateInvitationHandler(LeagueAccess access, InvitationRepository invitations, Clock clock) {
        this.access = access;
        this.invitations = invitations;
        this.clock = clock;
    }

    @Override
    public String handle(RegenerateInvitationCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Instant now = clock.instant();
        invitations.findActiveByLeagueId(command.leagueId()).ifPresent(active -> {
            active.revoke(now);
            invitations.save(active);
        });
        Invitation.Issued issued = Invitation.issue(new LeagueId(command.leagueId()), now);
        invitations.save(issued.invitation());
        return issued.token();
    }
}
