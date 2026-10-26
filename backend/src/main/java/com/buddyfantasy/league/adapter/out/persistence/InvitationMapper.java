package com.buddyfantasy.league.adapter.out.persistence;

import com.buddyfantasy.league.domain.Invitation;
import com.buddyfantasy.league.domain.LeagueId;
import lombok.experimental.UtilityClass;

@UtilityClass
class InvitationMapper {

    Invitation toDomain(InvitationEntity e) {
        return new Invitation(e.id, new LeagueId(e.leagueId), e.tokenHash, e.createdAt, e.revokedAt);
    }

    InvitationEntity toEntity(Invitation i) {
        return new InvitationEntity(i.id(), i.leagueId().value(), i.tokenHash(), i.createdAt(), i.revokedAt());
    }
}
