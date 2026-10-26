package com.buddyfantasy.league.application.usecases.listleaguemembers;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.shared.application.Query;

public record ListLeagueMembersQuery(UUID userId, UUID leagueId) implements Query<List<MemberView>> {
}
