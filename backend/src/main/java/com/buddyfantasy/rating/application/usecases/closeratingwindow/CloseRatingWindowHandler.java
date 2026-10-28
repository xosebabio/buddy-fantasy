package com.buddyfantasy.rating.application.usecases.closeratingwindow;

import java.time.Clock;
import java.util.Optional;

import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Locks the window before reading the votes, so a vote either commits before the close and counts or waits for it and
 * finds the window closed; a concurrent close waits and then finds it closed too.
 */
@Component
class CloseRatingWindowHandler implements CommandHandler<CloseRatingWindowCommand, Boolean> {

    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CloseRatingWindowHandler(RatingWindowRepository windows, RatingVoteRepository votes,
                             ApplicationEventPublisher events, Clock clock) {
        this.windows = windows;
        this.votes = votes;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Boolean handle(CloseRatingWindowCommand command) {
        Optional<RatingWindow> window = windows.lock(command.matchId());
        if (window.isEmpty() || !window.get().close(votes.findValuesByPlayer(command.matchId()), clock.instant())) {
            return false;
        }
        windows.save(window.get());
        window.get().pullEvents().forEach(events::publishEvent);
        return true;
    }
}
