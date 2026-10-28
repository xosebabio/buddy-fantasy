package com.buddyfantasy.rating.application.usecases.openratingwindow;

import com.buddyfantasy.league.LeagueSettingsProvider;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The league's parameters are only read if the match has no window yet; the insert decides between concurrent calls. */
@Component
@RequiredArgsConstructor
class OpenRatingWindowHandler implements CommandHandler<OpenRatingWindowCommand, Boolean> {

    private final RatingWindowRepository windows;
    private final LeagueSettingsProvider leagueSettings;

    @Override
    public Boolean handle(OpenRatingWindowCommand command) {
        if (windows.exists(command.matchId())) {
            return false;
        }
        return windows.saveIfAbsent(RatingWindow.open(command.match(), command.sheet(),
                leagueSettings.ratingSettings(command.leagueId()), command.occurredAt()));
    }
}
