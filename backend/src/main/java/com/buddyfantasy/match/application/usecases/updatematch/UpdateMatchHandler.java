package com.buddyfantasy.match.application.usecases.updatematch;

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
import com.buddyfantasy.match.domain.MatchError;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.MatchLimits;
import com.buddyfantasy.match.domain.Matchday;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * One request, one audit entry: {@code RESCHEDULED} if the matchday or the kickoff changed (it also carries any
 * change of opponent or venue), {@code DETAILS_UPDATED} otherwise, and none if nothing changed.
 */
@Component
@RequiredArgsConstructor
class UpdateMatchHandler implements CommandHandler<UpdateMatchCommand, Void> {

    private final LeagueAccess access;
    private final MatchRepository matches;
    private final MatchAuditLog audit;
    private final MatchLimitsRepository limits;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(UpdateMatchCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Match match = matches.findByLeagueIdAndId(command.leagueId(), new MatchId(command.matchId()))
                .orElseThrow(() -> new NotFoundException("Match " + command.matchId() + " not in league "
                        + command.leagueId()));
        Instant now = clock.instant();
        MatchSnapshot before = MatchSnapshot.of(match);
        MatchLimits matchLimits = limits.get();
        match.updateDetails(command.opponent().strip(), command.venue(), matchLimits);
        match.reschedule(new Matchday(command.matchday()), command.kickoffAt(), now, matchLimits);
        MatchSnapshot after = MatchSnapshot.of(match);
        if (after.equals(before)) {
            return null;
        }
        // keeping its own matchday or kickoff is not a clash
        boolean matchdayChanged = after.matchday() != before.matchday();
        boolean kickoffChanged = !after.kickoffAt().equals(before.kickoffAt());
        if (matchdayChanged && matches.existsMatchday(command.leagueId(), match.matchday())) {
            throw new ConflictException(MatchError.MATCHDAY_TAKEN, "Matchday already has a match");
        }
        if (kickoffChanged && matches.existsKickoff(command.leagueId(), match.kickoffAt())) {
            throw new ConflictException(MatchError.KICKOFF_TAKEN, "Another match already starts at that time");
        }
        matches.save(match);
        MatchAuditAction action = matchdayChanged || kickoffChanged
                ? MatchAuditAction.RESCHEDULED
                : MatchAuditAction.DETAILS_UPDATED;
        audit.append(new MatchAuditEntry(command.leagueId(), match.id().value(), command.userId(), action, now,
                before, after));
        match.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
