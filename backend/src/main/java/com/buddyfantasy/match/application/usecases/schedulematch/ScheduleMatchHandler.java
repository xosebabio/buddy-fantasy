package com.buddyfantasy.match.application.usecases.schedulematch;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.match.application.port.out.MatchAuditEntry;
import com.buddyfantasy.match.application.port.out.MatchAuditLog;
import com.buddyfantasy.match.application.port.out.MatchLimitsRepository;
import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.application.port.out.MatchSnapshot;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchAuditAction;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.Matchday;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ScheduleMatchHandler implements CommandHandler<ScheduleMatchCommand, MatchId> {

    private final LeagueAccess access;
    private final MatchRepository matches;
    private final MatchAuditLog audit;
    private final MatchLimitsRepository limits;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public MatchId handle(ScheduleMatchCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Instant now = clock.instant();
        Match match = Match.schedule(command.leagueId(), new Matchday(command.matchday()), command.opponent().strip(),
                command.venue(), command.kickoffAt(), now, limits.get());
        if (matches.existsMatchday(command.leagueId(), match.matchday())) {
            throw new ConflictException("Matchday already has a match");
        }
        if (matches.existsKickoff(command.leagueId(), match.kickoffAt())) {
            throw new ConflictException("Another match already starts at that time");
        }
        matches.save(match);
        audit.append(new MatchAuditEntry(command.leagueId(), match.id().value(), command.userId(),
                MatchAuditAction.SCHEDULED, now, null, MatchSnapshot.of(match)));
        match.pullEvents().forEach(events::publishEvent);
        return match.id();
    }
}
