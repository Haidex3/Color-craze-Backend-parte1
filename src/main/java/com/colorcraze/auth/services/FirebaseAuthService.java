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
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class FirebaseAuthService {

    private final JwtUtil jwtUtil;
    private final AuthUserRepository userRepository;

    public FirebaseAuthService(JwtUtil jwtUtil, AuthUserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

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
                    user.getUid(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole()
            );

            return new LoginResponse(jwt, refreshToken, ud);

        } catch (FirebaseAuthException e) {
            throw new FirebaseLoginException("Error validating Firebase ID token", e);
        }
    }

    public LoginResponse refresh(String refreshToken) {
        Optional<AuthUser> maybe = userRepository.findByRefreshToken(refreshToken);
        if (maybe.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        AuthUser user = maybe.get();

        String newRefresh = UUID.randomUUID().toString();
        String newJwt = jwtUtil.generateToken(user.getUid(), user.getRole());

        user.setRefreshToken(newRefresh);
        userRepository.save(user);

        UserData ud = new UserData(user.getId(), user.getUid(), user.getEmail(), user.getDisplayName(), user.getRole());

        return new LoginResponse(newJwt, newRefresh, ud);
    }
}

