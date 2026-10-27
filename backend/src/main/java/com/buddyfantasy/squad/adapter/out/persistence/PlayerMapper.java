package com.buddyfantasy.squad.adapter.out.persistence;

import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.PlayerPhotoKey;
import com.buddyfantasy.squad.domain.ShirtNumber;
import lombok.experimental.UtilityClass;

@UtilityClass
class PlayerMapper {

    Player toDomain(PlayerEntity e) {
        return new Player(new PlayerId(e.id), e.leagueId, e.name,
                e.shirtNumber == null ? null : new ShirtNumber(e.shirtNumber), e.position, e.initialValue,
                PlayerPhotoKey.ofNullable(e.photoKey), e.createdAt, e.retiredAt);
    }

    PlayerEntity toEntity(Player p) {
        return new PlayerEntity(p.id().value(), p.leagueId(), p.name(),
                p.shirtNumber() == null ? null : (short) p.shirtNumber().value(), p.position(), p.initialValue(),
                p.photoKey() == null ? null : p.photoKey().value(), p.createdAt(), p.retiredAt());
    }
}
