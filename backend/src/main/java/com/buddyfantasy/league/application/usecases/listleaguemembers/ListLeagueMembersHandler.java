package com.buddyfantasy.league.application.usecases.listleaguemembers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.identity.UserDirectory;
import com.buddyfantasy.identity.UserSummary;
import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.Membership;
import com.buddyfantasy.shared.application.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ListLeagueMembersHandler implements QueryHandler<ListLeagueMembersQuery, List<MemberView>> {

    private final LeagueAccess access;
    private final MembershipRepository memberships;
    private final UserDirectory users;

    @Override
    public List<MemberView> handle(ListLeagueMembersQuery query) {
        access.requireMember(query.userId(), query.leagueId());
        List<Membership> members = memberships.findByLeagueId(query.leagueId());
        Set<UUID> ids = members.stream().map(Membership::userId).collect(Collectors.toSet());
        Map<UUID, UserSummary> profiles = users.findSummaries(ids);
        return members.stream().map(m -> toView(m, profiles.get(m.userId()))).toList();
    }

    private static MemberView toView(Membership member, UserSummary profile) {
        return new MemberView(member.userId(), profile == null ? null : profile.displayName(),
                profile == null ? null : profile.avatarUrl(), member.role(), member.joinedAt());
    }
}
