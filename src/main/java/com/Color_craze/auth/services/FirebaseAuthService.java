package com.Color_craze.auth.services;


import com.Color_craze.auth.dtos.FirebaseLoginRequest;
import com.Color_craze.auth.dtos.LoginResponse;
import com.Color_craze.auth.dtos.UserData;
import com.Color_craze.auth.models.AuthUser;
import com.Color_craze.auth.repositories.AuthUserRepository;
import com.Color_craze.utils.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
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

    /**
     * Verifica idToken de Firebase, crea/actualiza usuario local y devuelve LoginResponse
     */
    public LoginResponse loginWithFirebase(FirebaseLoginRequest request) throws Exception {
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());

        String uid = decoded.getUid();
        String email = decoded.getEmail();
        String name = decoded.getName();

        // buscar usuario local por uid
        Optional<AuthUser> maybe = userRepository.findByUid(uid);

        AuthUser user;
        if (maybe.isPresent()) {
            user = maybe.get();
            // actualizar datos básicos si cambiaron
            user.setEmail(email);
            user.setDisplayName(name);
        } else {
            // crear
            String role = "USER"; // rol por defecto, puedes personalizar
            user = new AuthUser(uid, email, name, role, null);
        }

        // generar refresh token (uuid) y jwt interno
        String refreshToken = UUID.randomUUID().toString();
        String jwt = jwtUtil.generateToken(uid, user.getRole());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        UserData ud = new UserData(user.getId(), user.getUid(), user.getEmail(), user.getDisplayName(), user.getRole());

        return new LoginResponse(jwt, refreshToken, ud);
    }

    /**
     * Refresh token flow: recibe refresh token y devuelve nuevo JWT + refresh token
     */
    public LoginResponse refresh(String refreshToken) {
        // buscar usuario por refresh token
        Optional<AuthUser> maybe = userRepository.findByRefreshToken(refreshToken);
        if (maybe.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        AuthUser user = maybe.get();

        // crear nuevos tokens
        String newRefresh = UUID.randomUUID().toString();
        String newJwt = jwtUtil.generateToken(user.getUid(), user.getRole());

        user.setRefreshToken(newRefresh);
        userRepository.save(user);

        UserData ud = new UserData(user.getId(), user.getUid(), user.getEmail(), user.getDisplayName(), user.getRole());

        return new LoginResponse(newJwt, newRefresh, ud);
    }
}
