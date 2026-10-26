package com.buddyfantasy.identity.application.port.out;

import java.util.Optional;

import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByGoogleSub(String googleSub);

    void save(User user);
}
