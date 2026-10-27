package com.buddyfantasy.league.application.usecases.createleague;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.league.application.port.out.InvitationRepository;
import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Invitation;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class CreateLeagueHandler implements CommandHandler<CreateLeagueCommand, CreatedLeague> {

    private final LeagueRepository leagues;
    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final LeagueDefaultsRepository defaults;
    private final LeagueLimitsRepository limits;
    private final Clock clock;

    CreateLeagueHandler(LeagueRepository leagues, MembershipRepository memberships, InvitationRepository invitations,
                        LeagueDefaultsRepository defaults, LeagueLimitsRepository limits, Clock clock) {
        this.leagues = leagues;
        this.memberships = memberships;
        this.invitations = invitations;
        this.defaults = defaults;
        this.limits = limits;
        this.clock = clock;
    }

    @Override
    public CreatedLeague handle(CreateLeagueCommand command) {
        Instant now = clock.instant();
        League league = League.create(command.name().strip(), now, defaults.settings(), defaults.scoringRules(),
                limits.get());
        leagues.save(league);
        memberships.save(Membership.join(league.id(), command.creatorId(), LeagueRole.ADMIN, now));
        Invitation.Issued invitation = Invitation.issue(league.id(), now);
        invitations.save(invitation.invitation());
        return new CreatedLeague(league.id(), invitation.token());
    }
}
