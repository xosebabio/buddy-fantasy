package com.buddyfantasy.squad.application.usecases.registerplayer;

import java.time.Clock;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.ShirtNumber;
import com.buddyfantasy.squad.domain.SquadError;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RegisterPlayerHandler implements CommandHandler<RegisterPlayerCommand, PlayerId> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final SquadLimitsRepository limits;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public PlayerId handle(RegisterPlayerCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        ShirtNumber shirtNumber = ShirtNumber.ofNullable(command.shirtNumber());
        if (shirtNumber != null && players.existsActiveShirtNumber(command.leagueId(), shirtNumber)) {
            throw new ConflictException(SquadError.SHIRT_NUMBER_TAKEN,
                    "Shirt number already taken by an active player");
        }
        Player player = Player.register(command.leagueId(), command.name().strip(), shirtNumber, command.position(),
                command.initialValue(), clock.instant(), limits.get());
        players.save(player);
        player.pullEvents().forEach(events::publishEvent);
        return player.id();
    }
}
