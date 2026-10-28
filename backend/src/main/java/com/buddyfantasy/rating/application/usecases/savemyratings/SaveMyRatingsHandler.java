package com.buddyfantasy.rating.application.usecases.savemyratings;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.rating.application.RatingWindowLookup;
import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.application.usecases.getmatchratings.MatchRatingsView;
import com.buddyfantasy.rating.domain.MyRatings;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Locks the window in share mode before checking the deadline with the server's clock, so the votes and the close
 * take turns: a vote that got the lock first is saved and counts in the close; one that waited for the close finds
 * the window closed (409). Votes of several members go at once.
 */
@Component
@RequiredArgsConstructor
class SaveMyRatingsHandler implements CommandHandler<SaveMyRatingsCommand, MatchRatingsView> {

    private final LeagueAccess access;
    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final RatingWindowLookup lookup;
    private final Clock clock;

    @Override
    public MatchRatingsView handle(SaveMyRatingsCommand command) {
        access.requireMember(command.userId(), command.leagueId());
        RatingWindow window = lookup.require(windows.share(command.matchId()), command.leagueId(), command.matchId());
        Instant now = clock.instant();
        // the admin's exclusions (RF-41) come with ExcludeRaterCommand
        MyRatings ratings = votes.findMine(command.matchId(), command.userId())
                .replace(command.votes(), window, false, now);
        votes.save(ratings, now);
        return MatchRatingsView.from(window, ratings, now);
    }
}
