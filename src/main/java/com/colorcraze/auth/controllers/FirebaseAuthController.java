package com.colorcraze.auth.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.auth.dtos.FirebaseLoginRequest;
import com.colorcraze.auth.dtos.LoginResponse;
import com.colorcraze.auth.services.FirebaseAuthService;

@RestController
@RequestMapping("/api/auth")
public class FirebaseAuthController {
    
    private final FirebaseAuthService authService;

    public FirebaseAuthController(FirebaseAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<LoginResponse> firebaseLogin(@RequestBody FirebaseLoginRequest request) {
        LoginResponse resp = authService.loginWithFirebase(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/firebase-guest")
    public ResponseEntity<LoginResponse> firebaseGuest(@RequestBody FirebaseLoginRequest request) {
        return firebaseLogin(request);
    }

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
