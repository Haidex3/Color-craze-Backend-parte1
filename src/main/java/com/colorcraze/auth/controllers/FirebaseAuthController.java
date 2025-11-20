package com.colorcraze.auth.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.auth.dtos.FirebaseLoginRequest;
import com.colorcraze.auth.dtos.LoginResponse;
import com.colorcraze.auth.services.FirebaseAuthService;

/**
 * REST controller that exposes authentication endpoints backed by Firebase.
 * This controller handles user login, guest login, and token refresh operations.
 * All endpoints are mapped under /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
public class FirebaseAuthController {

    private final FirebaseAuthService authService;

    /**
     * Constructs a new {@link FirebaseAuthController} with the required authentication service.
     *
     * @param authService the Firebase-based authentication service
     */
    public FirebaseAuthController(FirebaseAuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user using Firebase credentials.
     * This endpoint receives a {@link FirebaseLoginRequest} containing an ID token
     * issued by Firebase, delegates validation to the {@link FirebaseAuthService},
     * and returns the generated {@link LoginResponse} containing authentication details.
     *
     * @param request the Firebase login request containing the Firebase ID token
     * @return a 200 OK response with login details if authentication succeeds
     */
    @PostMapping("/firebase-login")
    public ResponseEntity<LoginResponse> firebaseLogin(@RequestBody FirebaseLoginRequest request) {
        LoginResponse resp = authService.loginWithFirebase(request);
        return ResponseEntity.ok(resp);
    }

    /**
     * Authenticates or creates a guest user using Firebase credentials.
     * This endpoint behaves the same as {@link #firebaseLogin(FirebaseLoginRequest)}
     * but is intended for guest login flows within the application.
     *
     * @param request the Firebase login request for a guest session
     * @return a 200 OK response with login details if authentication succeeds
     */
    @PostMapping("/firebase-guest")
    public ResponseEntity<LoginResponse> firebaseGuest(@RequestBody FirebaseLoginRequest request) {
        return firebaseLogin(request);
    }

    /**
     * Refreshes the authentication tokens using a valid refresh token.
     * The request body must contain a "refreshToken" field. If absent,
     * a 400 Bad Request response is returned. Otherwise, the token is validated
     * and processed by the {@link FirebaseAuthService}.
     *
     * @param body a map containing the "refreshToken" key and its corresponding value
     * @return a 200 OK response with new JWT tokens, or 400 Bad Request if the token is missing
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }

        LoginResponse resp = authService.refresh(refreshToken);
        return ResponseEntity.ok(resp);
    }
}