package com.buddyfantasy.squad.application.usecases.confirmplayerphoto;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.DomainValidationException;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.ImageCleanup;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.application.port.out.StoredImage;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.PlayerPhotoFormat;
import com.buddyfantasy.squad.domain.PlayerPhotoKey;
import org.springframework.stereotype.Component;

@Component
class ConfirmPlayerPhotoHandler implements CommandHandler<ConfirmPlayerPhotoCommand, Void> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ImageStorage images;
    private final ImageCleanup cleanup;
    private final SquadLimitsRepository limits;

    ConfirmPlayerPhotoHandler(LeagueAccess access, PlayerRepository players, ImageStorage images,
                              ImageCleanup cleanup, SquadLimitsRepository limits) {
        this.access = access;
        this.players = players;
        this.images = images;
        this.cleanup = cleanup;
        this.limits = limits;
    }

    @Override
    public Void handle(ConfirmPlayerPhotoCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Player player = players.findByLeagueIdAndId(command.leagueId(), new PlayerId(command.playerId()))
                .orElseThrow(() -> new NotFoundException("Player not found"));
        player.requireActive();
        PlayerPhotoKey key = new PlayerPhotoKey(command.photoKey());
        // checked before asking the storage, so it is never probed with keys of other players or leagues
        if (!key.belongsTo(command.leagueId(), player.id())) {
            throw new DomainValidationException("Photo key does not belong to this player");
        }
        if (key.equals(player.photoKey())) {
            return null;
        }
        StoredImage stored = images.find(key.value())
                .orElseThrow(() -> new DomainValidationException("No photo was uploaded under that key"));
        PlayerPhotoFormat.requireAcceptable(stored.contentType(), stored.sizeBytes(), limits.get());
        PlayerPhotoKey previous = player.photoKey();
        player.changePhoto(key);
        players.save(player);
        if (previous != null) {
            cleanup.deleteAfterCommit(previous.value());
        }
        return null;
    }
}
