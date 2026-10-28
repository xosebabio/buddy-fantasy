package com.buddyfantasy.rating.application.usecases.closeratingwindow;

import java.util.UUID;

import com.buddyfantasy.shared.application.Command;

/**
 * RF-17, RF-22, RF-23: closes the rating window of a match whose time is up, from the close job: freezes each
 * player's result and publishes {@link com.buddyfantasy.rating.RatingWindowClosed}. Idempotent: returns true only for
 * the call that closed it; false if it was already closed, its time is not up or the match has no window.
 */
public record CloseRatingWindowCommand(UUID matchId) implements Command<Boolean> {
}
