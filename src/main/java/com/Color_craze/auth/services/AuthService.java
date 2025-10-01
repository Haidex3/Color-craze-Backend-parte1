package com.Color_craze.auth.services;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.Color_craze.auth.dtos.AuthResponse;
import com.Color_craze.auth.dtos.LoginRequest;
import com.Color_craze.auth.dtos.UserDetailsResponse;
import com.Color_craze.auth.models.AuthUser;
import com.Color_craze.auth.repositories.AuthRepository;
import com.Color_craze.configs.CustomUserDetails;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;

/**
 * Service for handling authentication operations such as login and token refresh.
 * Uses MongoDB for user persistence.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticates a user with email and password, generates tokens,
     * and returns an authentication response.
     */
    public AuthResponse login(LoginRequest request) {
        System.out.println("[DEBUG] Consultando en colección 'auths' el email: " + request.email());
        var userOpt = authRepository.findByEmail(request.email());
        System.out.println("[DEBUG] Resultado de la consulta: " + userOpt);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                )
            );
        } catch (Exception ex) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        AuthUser user = userOpt
            .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Usa el id como String, porque Mongo usa ObjectId/string, no UUID por defecto
        UserDetailsResponse userData = new UserDetailsResponse(
            user.getId(), user.getEmail(), user.getNickname()
        );

        return new AuthResponse(token, refreshToken, userData);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw new ExpiredJwtException(null, null, "Refresh token expirado");
        } catch (JwtException ex) {
            throw new SignatureException("Refresh token inválido");
        }

        AuthUser user = authRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new SignatureException("Refresh token inválido o expirado");
        }

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        UserDetailsResponse userData = new UserDetailsResponse(
            user.getId(), user.getEmail(), user.getNickname()
        );

        return new AuthResponse(newAccessToken, newRefreshToken, userData);
    }

    
    public UserDetailsResponse createTemporaryUser() {
            String id = UUID.randomUUID().toString(); 
            String email = "guest_" + id + "@example.com";
            String nickname = "Guest_" + id.substring(0, 8);
            return new UserDetailsResponse(id, email, nickname);
        }

        public AuthResponse createGuestToken() {
        String id = UUID.randomUUID().toString();
        String email = "guest_" + id + "@example.com";
        String nickname = "Guest_" + id.substring(0, 8);

        AuthUser guestAuthUser = AuthUser.builder()
                .id(id)
                .email(email)
                .nickname(nickname)
                .password("")
                .build();

        CustomUserDetails tempUserDetails = new CustomUserDetails(guestAuthUser);

        String token = jwtService.generateToken(tempUserDetails);
        String refreshToken = jwtService.generateRefreshToken(tempUserDetails);

        UserDetailsResponse guestUser = new UserDetailsResponse(id, email, nickname);

        return new AuthResponse(token, refreshToken, guestUser);
    }

}
