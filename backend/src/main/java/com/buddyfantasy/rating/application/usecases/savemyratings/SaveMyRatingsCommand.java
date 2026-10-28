package com.buddyfantasy.rating.application.usecases.savemyratings;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.rating.application.usecases.getmatchratings.MatchRatingsView;
import com.buddyfantasy.rating.domain.Vote;
import com.buddyfantasy.shared.application.Command;

/**
 * RF-18 to RF-20: a member (the admin rates too) replaces all their votes in a closed match while its window is open:
 * a player missing from {@code votes} loses the member's vote, so an empty list removes them all. Rating is optional
 * and per player. Returns the ratings as {@code GetMatchRatingsQuery} does.
 */
public record SaveMyRatingsCommand(UUID userId, UUID leagueId, UUID matchId, List<Vote> votes)
        implements Command<MatchRatingsView> {

    public SaveMyRatingsCommand {
        votes = List.copyOf(votes);
    }
}
