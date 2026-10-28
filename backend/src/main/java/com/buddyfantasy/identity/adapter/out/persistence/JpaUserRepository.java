package com.buddyfantasy.identity.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.buddyfantasy.identity.application.port.out.UserRepository;
import com.buddyfantasy.identity.domain.User;
import com.buddyfantasy.identity.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository jpa;

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<UserId> ids) {
        return jpa.findAllById(ids.stream().map(UserId::value).toList()).stream().map(UserMapper::toDomain).toList();
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
