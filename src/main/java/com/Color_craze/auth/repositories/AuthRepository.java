package com.Color_craze.auth.repositories;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Color_craze.auth.models.AuthUser;

public interface AuthRepository extends JpaRepository<AuthUser, String> {

    Optional<AuthUser> findByEmail(String email);

    boolean existsByEmail(String email);

}