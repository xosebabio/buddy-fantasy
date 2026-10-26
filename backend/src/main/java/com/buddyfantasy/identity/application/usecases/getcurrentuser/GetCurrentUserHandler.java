package com.buddyfantasy.identity.application.usecases.getcurrentuser;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
class GetCurrentUserHandler implements QueryHandler<GetCurrentUser, UserView> {

    private final UserRepository users;

    GetCurrentUserHandler(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserView handle(GetCurrentUser query) {
        User user = users.findById(new UserId(query.userId()))
                .orElseThrow(() -> new IllegalStateException("Session user not found: " + query.userId()));
        return new UserView(user.id().value(), user.displayName(), user.email(), user.avatarUrl());
    }
}
