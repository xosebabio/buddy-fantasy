package com.buddyfantasy.squad.application.usecases.retireplayer;

import java.time.Clock;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class RetirePlayerHandler implements CommandHandler<RetirePlayerCommand, Void> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    RetirePlayerHandler(LeagueAccess access, PlayerRepository players, ApplicationEventPublisher events,
                        Clock clock) {
        this.access = access;
        this.players = players;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Void handle(RetirePlayerCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Player player = players.findByLeagueIdAndId(command.leagueId(), new PlayerId(command.playerId()))
                .orElseThrow(() -> new NotFoundException("Player not found"));
        if (player.isRetired()) {
            return null;
        }
        player.retire(clock.instant());
        players.save(player);
        player.pullEvents().forEach(events::publishEvent);
        return null;
    }
}
