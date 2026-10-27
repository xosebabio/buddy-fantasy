package com.buddyfantasy.squad.application.usecases.requestplayerphotoupload;

import com.buddyfantasy.league.LeagueAccess;
import com.buddyfantasy.shared.application.CommandHandler;
import com.buddyfantasy.shared.domain.NotFoundException;
import com.buddyfantasy.squad.application.port.out.ImageStorage;
import com.buddyfantasy.squad.application.port.out.ImageUpload;
import com.buddyfantasy.squad.application.port.out.PlayerRepository;
import com.buddyfantasy.squad.application.port.out.SquadLimitsRepository;
import com.buddyfantasy.squad.domain.Player;
import com.buddyfantasy.squad.domain.PlayerId;
import com.buddyfantasy.squad.domain.PlayerPhotoFormat;
import com.buddyfantasy.squad.domain.PlayerPhotoKey;
import org.springframework.stereotype.Component;

@Component
class RequestPlayerPhotoUploadHandler implements CommandHandler<RequestPlayerPhotoUploadCommand, ImageUpload> {

    private final LeagueAccess access;
    private final PlayerRepository players;
    private final ImageStorage images;
    private final SquadLimitsRepository limits;

    RequestPlayerPhotoUploadHandler(LeagueAccess access, PlayerRepository players, ImageStorage images,
                                    SquadLimitsRepository limits) {
        this.access = access;
        this.players = players;
        this.images = images;
        this.limits = limits;
    }

    @Override
    public ImageUpload handle(RequestPlayerPhotoUploadCommand command) {
        access.requireAdmin(command.userId(), command.leagueId());
        Player player = players.findByLeagueIdAndId(command.leagueId(), new PlayerId(command.playerId()))
                .orElseThrow(() -> new NotFoundException("Player not found"));
        player.requireActive();
        PlayerPhotoFormat.requireAcceptable(command.contentType(), command.sizeBytes(), limits.get());
        PlayerPhotoKey key = PlayerPhotoKey.generate(command.leagueId(), player.id());
        return images.createUpload(key.value(), PlayerPhotoFormat.CONTENT_TYPE, command.sizeBytes());
    }
}
