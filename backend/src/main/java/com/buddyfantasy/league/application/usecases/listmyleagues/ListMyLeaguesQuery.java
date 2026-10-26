package com.buddyfantasy.league.application.usecases.listmyleagues;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record ListMyLeaguesQuery(UUID userId) implements Query<List<MyLeagueView>> {
}
