package com.buddyfantasy.rating.application.usecases.finddueratingwindows;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FindDueRatingWindowsHandler implements QueryHandler<FindDueRatingWindowsQuery, List<UUID>> {

    private final RatingWindowRepository windows;
    private final Clock clock;

    @Override
    public List<UUID> handle(FindDueRatingWindowsQuery query) {
        return windows.findDueMatchIds(clock.instant());
    }
}
