package com.colorcraze.auth.services;


import com.colorcraze.auth.dtos.FirebaseLoginRequest;
import com.colorcraze.auth.dtos.LoginResponse;
import com.colorcraze.auth.dtos.UserData;
import com.colorcraze.auth.exceptions.FirebaseLoginException;
import com.colorcraze.auth.models.AuthUser;
import com.colorcraze.auth.repositories.AuthUserRepository;
import com.colorcraze.utils.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for handling Firebase authentication operations.
 * Provides login and token refresh functionality using Firebase ID tokens and JWT.
 */
@Service
@AllArgsConstructor
public class FirebaseAuthService {

    private final JwtUtil jwtUtil;
    private final AuthUserRepository userRepository;

    /**
     * Authenticates a user with Firebase ID token.
     * If the user does not exist, a new user is created.
     * Generates a JWT and a refresh token for the authenticated user.
     *
     * @param request the Firebase login request containing the ID token
     * @return a {@link LoginResponse} with JWT, refresh token, and user data
     * @throws FirebaseLoginException if Firebase token validation fails
     */
    public LoginResponse loginWithFirebase(FirebaseLoginRequest request) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());

            String uid = decoded.getUid();
            String email = decoded.getEmail();
            String name = decoded.getName();

            Optional<AuthUser> maybe = userRepository.findByUid(uid);

            AuthUser user;
            if (maybe.isPresent()) {
                user = maybe.get();
                user.setEmail(email);
                user.setDisplayName(name);
            } else {
                String role = "USER";
                user = new AuthUser(uid, email, name, role, null);
            }

            String refreshToken = UUID.randomUUID().toString();
            String jwt = jwtUtil.generateToken(uid, user.getRole());

            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            UserData ud = new UserData(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
            );

            return new LoginResponse(jwt, refreshToken, ud);

        } catch (FirebaseAuthException e) {
            throw new FirebaseLoginException("Error validating Firebase ID token", e);
        }
    }

    /**
     * Refreshes the authentication tokens for a user given a valid refresh token.
     * Generates a new JWT and refresh token and updates the user record.
     *
     * @param refreshToken the current refresh token
     * @return a {@link LoginResponse} with new JWT, refresh token, and user data
     * @throws IllegalArgumentException if the refresh token is invalid
     */
    public LoginResponse refresh(String refreshToken) {
        Optional<AuthUser> maybe = userRepository.findByRefreshToken(refreshToken);
        if (maybe.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        AuthUser user = maybe.get();

        String newRefresh = UUID.randomUUID().toString();
        String newJwt = jwtUtil.generateToken(user.getId(), user.getRole());

        user.setRefreshToken(newRefresh);
        userRepository.save(user);

        UserData ud = new UserData(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());

        return new LoginResponse(newJwt, newRefresh, ud);
    }
}

