package com.buddyfantasy.lineup.application.usecases.deletematchlock;

import com.buddyfantasy.lineup.application.port.out.MatchLineupLockRepository;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class DeleteMatchLockHandler implements CommandHandler<DeleteMatchLockCommand, Void> {

    private final MatchLineupLockRepository locks;

    DeleteMatchLockHandler(MatchLineupLockRepository locks) {
        this.locks = locks;
    }

    @Override
    public Void handle(DeleteMatchLockCommand command) {
        locks.deleteByMatchId(command.matchId());
        return null;
    }
}
