package com.buddyfantasy.rating.application.usecases.excluderater;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.rating.application.RatingWindowLookup;
import com.buddyfantasy.rating.application.port.out.RatingExclusionRepository;
import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Locks the window ({@code for update}) before reading the exclusions and deleting the votes, so it never crosses a
 * vote of the same member: a vote that got the lock first is deleted here; one that waited finds the exclusion (409).
 * It holds back the votes of the match for an instant, which is fine for such a rare operation.
 */
@Component
@RequiredArgsConstructor
class ExcludeRaterHandler implements CommandHandler<ExcludeRaterCommand, Void> {

    private final LeagueAccess access;
    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final RatingExclusionRepository exclusions;
    private final RatingWindowLookup lookup;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(ExcludeRaterCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        if (access.roleOf(command.raterId(), command.leagueId()).isEmpty()) {
            throw new NotFoundException("User " + command.raterId() + " is not a member of league "
                    + command.leagueId());
        }
        RatingWindow window = lookup.require(windows.lock(command.matchId()), command.leagueId(), command.matchId());
        Instant now = clock.instant();
        window.exclude(command.raterId(), command.userId(),
                        exclusions.findExcludedUserIds(command.matchId()).contains(command.raterId()), now)
                .ifPresent(exclusion -> {
                    votes.delete(command.matchId(), command.raterId());
                    exclusions.save(exclusion);
                    window.pullEvents().forEach(events::publishEvent);
                });
        return null;
    }
}
