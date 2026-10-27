package com.buddyfantasy.match.application.usecases.closematch;

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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** The deadline is checked against the server clock. Closing an already closed match changes and logs nothing. */
@Component
class CloseMatchHandler implements CommandHandler<CloseMatchCommand, Void> {

    private final LeagueAccess access;
    private final MatchRepository matches;
    private final MatchAuditLog audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CloseMatchHandler(LeagueAccess access, MatchRepository matches, MatchAuditLog audit,
                      ApplicationEventPublisher events, Clock clock) {
        this.access = access;
        this.matches = matches;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Void handle(CloseMatchCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Match match = matches.findByLeagueIdAndId(command.leagueId(), new MatchId(command.matchId()))
                .orElseThrow(() -> new NotFoundException("Match " + command.matchId() + " not in league "
                        + command.leagueId()));
        if (match.isClosed()) {
            return null;
        }
        Instant now = clock.instant();
        MatchSnapshot before = MatchSnapshot.of(match);
        match.close(now);
        matches.save(match);
        audit.append(new MatchAuditEntry(command.leagueId(), match.id().value(), command.userId(),
                MatchAuditAction.CLOSED, now, before, MatchSnapshot.of(match)));
        match.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
