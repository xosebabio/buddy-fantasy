package com.buddyfantasy.identity.application;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.buddyfantasy.identity.UserDirectory;
import com.buddyfantasy.identity.UserSummary;
import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import org.springframework.stereotype.Service;

@Service
class UserDirectoryService implements UserDirectory {

    private final UserRepository users;

    UserDirectoryService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Map<UUID, UserSummary> findSummaries(Set<UUID> userIds) {
        Set<UserId> ids = userIds.stream().map(UserId::new).collect(Collectors.toSet());
        return users.findAllById(ids).stream()
                .map(UserDirectoryService::toSummary)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.id().value(), user.displayName(), user.avatarUrl());
    }
}
