package com.colorcraze.auth.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.colorcraze.auth.models.AuthUser;

import java.util.Optional;

/**
 * Repository interface for accessing and managing AuthUser entities in MongoDB.
 * Provides lookup operations for authentication and user management.
 */
public interface AuthUserRepository extends MongoRepository<AuthUser, String> {
    Optional<AuthUser> findById(String uid);
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByRefreshToken(String refreshToken);
}
