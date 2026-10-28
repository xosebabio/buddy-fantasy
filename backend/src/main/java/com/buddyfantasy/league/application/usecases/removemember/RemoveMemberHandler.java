package com.buddyfantasy.league.application.usecases.removemember;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RemoveMemberHandler implements CommandHandler<RemoveMemberCommand, Void> {

    private final LeagueAccess access;
    private final MembershipRepository memberships;

    @Override
    public Void handle(RemoveMemberCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Membership member = memberships.findByLeagueIdAndUserId(command.leagueId(), command.memberId())
                .orElseThrow(() -> new NotFoundException("Member not found"));
        member.requireRemovable();
        memberships.delete(member);
        return null;
    }
}
