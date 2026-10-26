package com.buddyfantasy.identity.adapter.out.persistence;

import java.util.Optional;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository jpa;

    JpaUserRepository(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByGoogleSub(String googleSub) {
        return jpa.findByGoogleSub(googleSub).map(UserMapper::toDomain);
    }

    @Override
    public void save(User user) {
        jpa.save(UserMapper.toEntity(user));
    }
}
