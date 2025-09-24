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
 * Handles user verification, JWT generation, and assembling authentication responses.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticates a user with email and password, generates access and refresh tokens,
     * and returns an authentication response containing tokens and user details.
     *
     * @param request The login request containing email and password.
     * @return AuthResponse containing access token, refresh token, and user details.
     * @throws BadCredentialsException if authentication fails or user is not found.
     */
    public AuthResponse login(LoginRequest request) {

        // 🔍 Depuración: imprime lo que llega desde el front
        System.out.println("[DEBUG] Email recibido: " + request.email());
        System.out.println("[DEBUG] Password recibido (texto plano): " + request.password());

        // 🔍 Genera un hash temporal para ver cómo quedaría encriptado
        String tempHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.password());
        System.out.println("[DEBUG] Hash BCrypt temporal: " + tempHash);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        AuthUser user = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        UserDetailsResponse userData = new UserDetailsResponse(
                UUID.fromString(user.getId()), user.getEmail(), user.getName());

        return new AuthResponse(token, refreshToken, userData);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * Verifies the refresh token, generates new access and refresh tokens,
     * and returns an updated authentication response.
     *
     * @param refreshToken The refresh token.
     * @return AuthResponse containing new access token, new refresh token, and user details.
     * @throws ExpiredJwtException if the refresh token has expired.
     * @throws SignatureException if the refresh token is invalid.
     * @throws BadCredentialsException if the user associated with the token is not found.
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
                UUID.fromString(user.getId()), user.getEmail(), user.getName());

        return new AuthResponse(newAccessToken, newRefreshToken, userData);
    }

}