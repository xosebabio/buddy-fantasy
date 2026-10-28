package com.buddyfantasy.match.application.usecases.deletematch;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.match.application.port.out.MatchAuditEntry;
import com.buddyfantasy.match.application.port.out.MatchAuditLog;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchAuditAction;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DeleteMatchHandler implements CommandHandler<DeleteMatchCommand, Void> {

    private final LeagueAccess access;
    private final MatchRepository matches;
    private final MatchAuditLog audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(DeleteMatchCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Match match = matches.findByLeagueIdAndId(command.leagueId(), new MatchId(command.matchId()))
                .orElseThrow(() -> new NotFoundException("Match " + command.matchId() + " not in league "
                        + command.leagueId()));
        Instant now = clock.instant();
        MatchSnapshot before = MatchSnapshot.of(match);
        match.delete(now);
        matches.delete(match);
        audit.append(new MatchAuditEntry(command.leagueId(), match.id().value(), command.userId(),
                MatchAuditAction.DELETED, now, before, null));
        match.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
