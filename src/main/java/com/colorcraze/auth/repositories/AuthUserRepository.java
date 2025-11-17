package com.colorcraze.auth.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.colorcraze.auth.models.AuthUser;

import java.util.Optional;

public interface AuthUserRepository extends MongoRepository<AuthUser, String> {
    Optional<AuthUser> findByUid(String uid);
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByRefreshToken(String refreshToken);
}
