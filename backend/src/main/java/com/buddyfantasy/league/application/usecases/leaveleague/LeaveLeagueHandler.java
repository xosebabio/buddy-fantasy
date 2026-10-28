package com.buddyfantasy.league.application.usecases.leaveleague;

import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LeaveLeagueHandler implements CommandHandler<LeaveLeagueCommand, Void> {

    private final MembershipRepository memberships;

    @Override
    public Void handle(LeaveLeagueCommand command) {
        Membership member = memberships.findByLeagueIdAndUserId(command.leagueId(), command.userId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + command.leagueId()));
        member.requireRemovable();
        memberships.delete(member);
        return null;
    }
}
