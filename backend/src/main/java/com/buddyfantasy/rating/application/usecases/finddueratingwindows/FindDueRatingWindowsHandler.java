package com.buddyfantasy.rating.application.usecases.finddueratingwindows;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
class FindDueRatingWindowsHandler implements QueryHandler<FindDueRatingWindowsQuery, List<UUID>> {

    private final RatingWindowRepository windows;
    private final Clock clock;

    FindDueRatingWindowsHandler(RatingWindowRepository windows, Clock clock) {
        this.windows = windows;
        this.clock = clock;
    }

    @Override
    public List<UUID> handle(FindDueRatingWindowsQuery query) {
        return windows.findDueMatchIds(clock.instant());
    }
}
