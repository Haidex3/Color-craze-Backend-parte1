package com.Color_craze.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Color_craze.auth.dtos.FirebaseLoginRequest;
import com.Color_craze.auth.dtos.LoginResponse;
import com.Color_craze.auth.services.FirebaseAuthService;

@RestController
@RequestMapping("/api/auth")
public class FirebaseAuthController {

    private final FirebaseAuthService authService;

    public FirebaseAuthController(FirebaseAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<LoginResponse> firebaseLogin(@RequestBody FirebaseLoginRequest request) throws Exception {
        LoginResponse resp = authService.loginWithFirebase(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/firebase-guest")
    public ResponseEntity<LoginResponse> firebaseGuest(@RequestBody FirebaseLoginRequest request) throws Exception {
        // same flow: verify token (anonymous) and create local guest user
        LoginResponse resp = authService.loginWithFirebase(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody java.util.Map<String,String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }
        LoginResponse resp = authService.refresh(refreshToken);
        return ResponseEntity.ok(resp);
    }
}
