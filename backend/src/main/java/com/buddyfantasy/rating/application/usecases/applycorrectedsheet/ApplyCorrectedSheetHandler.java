package com.buddyfantasy.rating.application.usecases.applycorrectedsheet;

import java.time.Clock;
import java.util.Optional;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.UnexpectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Locks the window, so a correction and the close take turns. If there is none it opens it with the corrected sheet;
 * if a concurrent {@code MatchClosed} opens it first, the insert waits for it and the correction is applied on top.
 */
@Component
@RequiredArgsConstructor
class ApplyCorrectedSheetHandler implements CommandHandler<ApplyCorrectedSheetCommand, Void> {

    private final RatingWindowRepository windows;
    private final LeagueSettingsProvider leagueSettings;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public Void handle(ApplyCorrectedSheetCommand command) {
        Optional<RatingWindow> locked = windows.lock(command.matchId());
        if (locked.isEmpty()) {
            if (windows.saveIfAbsent(RatingWindow.open(command.match(), command.sheet(),
                    leagueSettings.ratingSettings(command.leagueId()), command.occurredAt()))) {
                return null;
            }
            // a concurrent MatchClosed opened it first: the correction goes on top
            locked = windows.lock(command.matchId());
        }
        RatingWindow window = locked
                .orElseThrow(() -> new UnexpectedException("Rating window not found: " + command.matchId()));
        if (!window.applySheet(command.sheet(), command.occurredAt(), clock.instant())) {
            return null;
        }
        windows.save(window);
        window.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
