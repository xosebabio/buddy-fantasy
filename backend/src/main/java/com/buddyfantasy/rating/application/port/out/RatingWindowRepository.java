package com.buddyfantasy.rating.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.rating.domain.RatingWindow;

/**
 * The rating windows of closed matches with their players and results (RF-17), at most one per match. Every write
 * first locks the window's row until the transaction ends, so a correction of the sheet and the close of the window
 * take turns and the second one sees what the first one wrote.
 */
public interface RatingWindowRepository {

    /**
     * Inserts the window with its players unless the match already has one, also by a concurrent transaction. True
     * only for the call that inserted it.
     */
    boolean saveIfAbsent(RatingWindow window);

    boolean exists(UUID matchId);

    /** Locks the window ({@code for update}) and returns it with its players and results; empty if there is none. */
    Optional<RatingWindow> lock(UUID matchId);

    /**
     * Updates the stored window with {@code window}: its close, its copy of the sheet and the results. A player no
     * longer in the sheet is deleted with his votes; the others keep theirs.
     */
    void save(RatingWindow window);

    /** The matches whose window is not closed and whose {@code closesAt} is not after {@code now}, by closesAt. */
    List<UUID> findDueMatchIds(Instant now);
}
