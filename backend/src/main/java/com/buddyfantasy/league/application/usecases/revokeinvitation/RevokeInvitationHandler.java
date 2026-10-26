package com.buddyfantasy.league.application.usecases.revokeinvitation;

import java.time.Clock;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class RevokeInvitationHandler implements CommandHandler<RevokeInvitationCommand, Void> {

    private final LeagueAccess access;
    private final InvitationRepository invitations;
    private final Clock clock;

    RevokeInvitationHandler(LeagueAccess access, InvitationRepository invitations, Clock clock) {
        this.access = access;
        this.invitations = invitations;
        this.clock = clock;
    }

    @Override
    public Void handle(RevokeInvitationCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        invitations.findActiveByLeagueId(command.leagueId()).ifPresent(active -> {
            active.revoke(clock.instant());
            invitations.save(active);
        });
        return null;
    }
}
