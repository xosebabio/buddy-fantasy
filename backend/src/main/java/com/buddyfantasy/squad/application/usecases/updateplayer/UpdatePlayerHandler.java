package com.buddyfantasy.squad.application.usecases.updateplayer;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.ConflictException;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.ShirtNumber;
import org.springframework.stereotype.Component;

/**
 * The initial value stays editable for now. Once the first matchday is closed it must be locked and only
 * {@code market} will move the value (plan 04, confirmed decision 1).
 */
@Component
class UpdatePlayerHandler implements CommandHandler<UpdatePlayerCommand, Void> {

    private final LeagueAccess access;
    private final PlayerRepository players;

    UpdatePlayerHandler(LeagueAccess access, PlayerRepository players) {
        this.access = access;
        this.players = players;
    }

    @Override
    public Void handle(UpdatePlayerCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Player player = players.findByLeagueIdAndId(command.leagueId(), new PlayerId(command.playerId()))
                .orElseThrow(() -> new NotFoundException("Player not found"));
        ShirtNumber shirtNumber = ShirtNumber.ofNullable(command.shirtNumber());
        // keeping their own number is not a clash
        if (shirtNumber != null && !shirtNumber.equals(player.shirtNumber())
                && players.existsActiveShirtNumber(command.leagueId(), shirtNumber)) {
            throw new ConflictException("Shirt number already taken by an active player");
        }
        player.update(command.name().strip(), shirtNumber, command.position(), command.initialValue(),
                player.photoKey());
        players.save(player);
        return null;
    }
}
