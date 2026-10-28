package com.buddyfantasy.squad.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.ShirtNumber;
import com.buddyfantasy.squad.domain.SquadError;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPlayerRepository implements PlayerRepository {

    /** Partial unique index of V7 on {@code (league_id, shirt_number)} among active players. */
    private static final String SHIRT_NUMBER_INDEX = "squad_player_active_shirt_number_idx";

    private final SpringDataPlayerRepository jpa;

    @Override
    public Optional<Player> findByLeagueIdAndId(UUID leagueId, PlayerId id) {
        return jpa.findByLeagueIdAndId(leagueId, id.value()).map(PlayerMapper::toDomain);
    }

    @Override
    public List<Player> findByLeagueId(UUID leagueId, boolean includeRetired) {
        List<PlayerEntity> entities = includeRetired
                ? jpa.findByLeagueId(leagueId)
                : jpa.findByLeagueIdAndRetiredAtIsNull(leagueId);
        return entities.stream().map(PlayerMapper::toDomain).toList();
    }

    @Override
    public List<Player> findByLeagueIdAndIds(UUID leagueId, Collection<PlayerId> ids) {
        List<UUID> values = ids.stream().map(PlayerId::value).toList();
        return jpa.findByLeagueIdAndIdIn(leagueId, values).stream().map(PlayerMapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveShirtNumber(UUID leagueId, ShirtNumber shirtNumber) {
        return jpa.existsByLeagueIdAndShirtNumberAndRetiredAtIsNull(leagueId, (short) shirtNumber.value());
    }

    @Override
    public void save(Player player) {
        try {
            jpa.saveAndFlush(PlayerMapper.toEntity(player));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains(SHIRT_NUMBER_INDEX)) {
                throw new ConflictException(SquadError.SHIRT_NUMBER_TAKEN,
                        "Shirt number already taken by an active player");
            }
            throw e;
        }
    }
}
