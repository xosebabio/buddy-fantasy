package com.buddyfantasy.rating.application.usecases.savemyratings;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.LeagueRole;
import com.buddyfantasy.rating.application.RatingWindowLookup;
import com.buddyfantasy.rating.application.port.out.RatingExclusionRepository;
import com.buddyfantasy.rating.application.port.out.RatingVoteRepository;
import com.buddyfantasy.rating.application.port.out.RatingWindowRepository;
import com.buddyfantasy.rating.application.usecases.getmatchratings.MatchRatingsView;
import com.buddyfantasy.rating.domain.MyRatings;
import com.buddyfantasy.rating.domain.RatingWindow;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Locks the window in share mode before checking the deadline with the server's clock, so the votes and the close
 * take turns: a vote that got the lock first is saved and counts in the close; one that waited for the close finds
 * the window closed (409). Votes of several members go at once. The exclusions are read after the lock too: a vote
 * that waited for the admin's exclusion of its member finds it (409).
 */
@Component
@RequiredArgsConstructor
class SaveMyRatingsHandler implements CommandHandler<SaveMyRatingsCommand, MatchRatingsView> {

    private final LeagueAccess access;
    private final RatingWindowRepository windows;
    private final RatingVoteRepository votes;
    private final RatingExclusionRepository exclusions;
    private final RatingWindowLookup lookup;
    private final Clock clock;

    @Override
    public MatchRatingsView handle(SaveMyRatingsCommand command) {
        LeagueRole role = access.roleOf(command.userId(), command.leagueId())
                .orElseThrow(() -> new ForbiddenException("Not a member of league " + command.leagueId()));
        RatingWindow window = lookup.require(windows.share(command.matchId()), command.leagueId(), command.matchId());
        Instant now = clock.instant();
        List<UUID> excluded = exclusions.findExcludedUserIds(command.matchId());
        MyRatings ratings = votes.findMine(command.matchId(), command.userId())
                .replace(command.votes(), window, excluded.contains(command.userId()), now);
        votes.save(ratings, now);
        return MatchRatingsView.from(window, ratings, excluded, role == LeagueRole.ADMIN, now);
    }
}
