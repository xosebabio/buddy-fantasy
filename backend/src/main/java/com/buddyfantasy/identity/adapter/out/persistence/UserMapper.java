package com.buddyfantasy.identity.adapter.out.persistence;

import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import lombok.experimental.UtilityClass;

@UtilityClass
class UserMapper {

    User toDomain(UserEntity e) {
        return new User(new UserId(e.id), e.googleSub, e.email, e.displayName, e.avatarUrl, e.createdAt, e.lastLoginAt);
    }

    UserEntity toEntity(User u) {
        return new UserEntity(u.id().value(), u.googleSub(), u.email(), u.displayName(), u.avatarUrl(),
                u.createdAt(), u.lastLoginAt());
    }
}
