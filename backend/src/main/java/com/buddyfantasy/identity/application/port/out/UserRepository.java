package com.buddyfantasy.identity.application.port.out;

import java.util.Optional;

import com.buddyfantasy.identity.domain.User;

public interface UserRepository {

    Optional<User> findByGoogleSub(String googleSub);

    void save(User user);
}
