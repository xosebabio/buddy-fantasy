package com.buddyfantasy.identity.application.usecases.registerorloginuser;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
class RegisterOrLoginUserHandler implements CommandHandler<RegisterOrLoginUser, UserId> {

    private final UserRepository users;
    private final Clock clock;

    RegisterOrLoginUserHandler(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    public UserId handle(RegisterOrLoginUser command) {
        Instant now = clock.instant();
        User user = users.findByGoogleSub(command.googleSub()).orElse(null);
        if (user == null) {
            user = User.register(command.googleSub(), command.email(), command.name(), command.avatarUrl(), now);
        } else {
            user.recordLogin(now);
        }
        users.save(user);
        return user.id();
    }
}
