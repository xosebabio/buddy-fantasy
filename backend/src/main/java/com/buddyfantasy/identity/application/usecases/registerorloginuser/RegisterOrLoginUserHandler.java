package com.buddyfantasy.identity.application.usecases.registerorloginuser;

import java.time.Clock;
import java.time.Instant;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RegisterOrLoginUserHandler implements CommandHandler<RegisterOrLoginUserCommand, UserId> {

    private final UserRepository users;
    private final Clock clock;

    @Override
    public UserId handle(RegisterOrLoginUserCommand command) {
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
