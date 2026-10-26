package com.buddyfantasy.league.adapter.out.persistence;

import com.buddyfantasy.league.domain.LeagueId;
import com.buddyfantasy.league.domain.Membership;
import lombok.experimental.UtilityClass;

@UtilityClass
class MembershipMapper {

    Membership toDomain(MembershipEntity e) {
        return new Membership(e.id, new LeagueId(e.leagueId), e.userId, e.role, e.joinedAt);
    }

    MembershipEntity toEntity(Membership m) {
        return new MembershipEntity(m.id(), m.leagueId().value(), m.userId(), m.role(), m.joinedAt());
    }
}
