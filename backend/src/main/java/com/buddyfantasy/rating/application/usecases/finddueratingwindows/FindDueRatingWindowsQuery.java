package com.buddyfantasy.rating.application.usecases.finddueratingwindows;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

/**
 * For the close job: the matches of every league whose rating window is not closed yet and whose time is up
 * ({@code closesAt ≤ now}), ordered by closesAt.
 */
public record FindDueRatingWindowsQuery() implements Query<List<UUID>> {
}
