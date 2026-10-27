package com.buddyfantasy.squad.application.usecases.removeplayerphoto;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.ImageCleanup;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.PlayerPhotoKey;
import org.springframework.stereotype.Component;

@Component
class RemovePlayerPhotoHandler implements CommandHandler<RemovePlayerPhotoCommand, Void> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ImageCleanup cleanup;

    RemovePlayerPhotoHandler(LeagueAccess access, PlayerRepository players, ImageCleanup cleanup) {
        this.access = access;
        this.players = players;
        this.cleanup = cleanup;
    }

    @Override
    public Void handle(RemovePlayerPhotoCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Player player = players.findByLeagueIdAndId(command.leagueId(), new PlayerId(command.playerId()))
                .orElseThrow(() -> new NotFoundException("Player not found"));
        PlayerPhotoKey previous = player.photoKey();
        if (previous == null) {
            return null;
        }
        player.removePhoto();
        players.save(player);
        cleanup.deleteAfterCommit(previous.value());
        return null;
    }
}
