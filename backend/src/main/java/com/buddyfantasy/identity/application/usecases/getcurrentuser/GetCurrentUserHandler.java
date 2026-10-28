package com.buddyfantasy.identity.application.usecases.getcurrentuser;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import com.buddyfantasy.shared.application.QueryHandler;
import com.buddyfantasy.shared.domain.UnexpectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class GetCurrentUserHandler implements QueryHandler<GetCurrentUserQuery, UserView> {

    private final UserRepository users;

    @Override
    public UserView handle(GetCurrentUserQuery query) {
        User user = users.findById(new UserId(query.userId()))
                .orElseThrow(() -> new UnexpectedException("Session user not found: " + query.userId()));
        return new UserView(user.id().value(), user.displayName(), user.email(), user.avatarUrl());
    }
}
