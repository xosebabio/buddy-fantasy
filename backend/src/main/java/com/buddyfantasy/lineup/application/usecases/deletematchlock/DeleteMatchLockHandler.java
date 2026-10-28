package com.buddyfantasy.lineup.application.usecases.deletematchlock;

import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DeleteMatchLockHandler implements CommandHandler<DeleteMatchLockCommand, Void> {

    private final MatchLineupLockRepository locks;

    @Override
    public Void handle(DeleteMatchLockCommand command) {
        locks.deleteByMatchId(command.matchId());
        return null;
    }
}
