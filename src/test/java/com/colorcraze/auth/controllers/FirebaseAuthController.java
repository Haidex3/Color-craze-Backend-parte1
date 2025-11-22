package com.colorcraze.auth.controllers;

import com.colorcraze.auth.dtos.FirebaseLoginRequest;
import com.colorcraze.auth.dtos.LoginResponse;
import com.colorcraze.auth.dtos.UserData;
import com.colorcraze.auth.services.FirebaseAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthControllerTest {

    @Mock
    private FirebaseAuthService authService;

    @InjectMocks
    private FirebaseAuthController authController;

    @Test
    void firebaseLogin_Success() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        LoginResponse expectedResponse = new LoginResponse(
            "jwt-token",
            "refresh-token",
            new UserData("user-id", "user@example.com", "Test User", "USER")
        );

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseLogin(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        
        verify(authService).loginWithFirebase(request);
    }

    @Test
    void firebaseGuest_Success() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-guest-token");
        LoginResponse expectedResponse = new LoginResponse(
            "guest-jwt-token",
            "guest-refresh-token",
            new UserData("guest-id", "guest@example.com", "Guest User", "GUEST")
        );

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseGuest(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        
        verify(authService).loginWithFirebase(request);
    }

    @Test
    void refresh_ValidToken_Success() {
        String refreshToken = "valid-refresh-token";
        Map<String, String> requestBody = Map.of("refreshToken", refreshToken);
        LoginResponse expectedResponse = new LoginResponse(
            "new-jwt-token",
            "new-refresh-token",
            new UserData("user-id", "user@example.com", "Test User", "USER")
        );

        when(authService.refresh(refreshToken)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.refresh(requestBody);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        
        verify(authService).refresh(refreshToken);
    }

    @Test
    void refresh_MissingToken_ReturnsBadRequest() {
        Map<String, String> requestBody = Map.of("wrongKey", "some-value");

        ResponseEntity<LoginResponse> response = authController.refresh(requestBody);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(authService, never()).refresh(anyString());
    }


    @Test
    void refresh_EmptyToken_ReturnsSuccess() {
        String refreshToken = "";
        Map<String, String> requestBody = Map.of("refreshToken", refreshToken);
        LoginResponse expectedResponse = new LoginResponse(
            "new-jwt-token",
            "new-refresh-token",
            new UserData("user-id", "user@example.com", "Test User", "USER")
        );

        when(authService.refresh(refreshToken)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.refresh(requestBody);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        
        verify(authService).refresh(refreshToken);
    }

    @Test
    void refresh_WithComplexUserData_Success() {
        String refreshToken = "complex-refresh-token";
        Map<String, String> requestBody = Map.of("refreshToken", refreshToken);
        String userId = UUID.randomUUID().toString();
        LoginResponse expectedResponse = new LoginResponse(
            "complex-jwt-token",
            "complex-refresh-token",
            new UserData(userId, "complex.user@example.com", "Complex User Name", "ADMIN")
        );

        when(authService.refresh(refreshToken)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.refresh(requestBody);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        UserData userData = response.getBody().getUserData();
        assertNotNull(userData);
        assertEquals(userId, userData.getId());
        assertEquals("complex.user@example.com", userData.getEmail());
        assertEquals("Complex User Name", userData.getDisplayName());
        assertEquals("ADMIN", userData.getRole());
        
        verify(authService).refresh(refreshToken);
    }

    @Test
    void firebaseLogin_WithNullUserData_Success() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-token");
        LoginResponse expectedResponse = new LoginResponse("jwt-token", "refresh-token", null);

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseLogin(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getUserData());
        
        verify(authService).loginWithFirebase(request);
    }

    @Test
    void firebaseGuest_WithNullUserData_Success() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("guest-token");
        LoginResponse expectedResponse = new LoginResponse("guest-jwt", "guest-refresh", null);

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseGuest(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getUserData());
        
        verify(authService).loginWithFirebase(request);
    }

    @Test
    void refresh_EmptyRequestBody_ReturnsBadRequest() {
        Map<String, String> requestBody = Map.of();

        ResponseEntity<LoginResponse> response = authController.refresh(requestBody);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(authService, never()).refresh(anyString());
    }

    @Test
    void firebaseLogin_VerifyServiceCall() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("test-token");
        LoginResponse expectedResponse = new LoginResponse(
            "test-jwt",
            "test-refresh",
            new UserData("test-id", "test@example.com", "Test User", "USER")
        );

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseLogin(request);

        verify(authService, times(1)).loginWithFirebase(request);
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void firebaseGuest_VerifyServiceCall() {
        FirebaseLoginRequest request = new FirebaseLoginRequest("guest-token");
        LoginResponse expectedResponse = new LoginResponse(
            "guest-jwt",
            "guest-refresh",
            new UserData("guest-id", "guest@example.com", "Guest User", "GUEST")
        );

        when(authService.loginWithFirebase(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.firebaseGuest(request);

        verify(authService, times(1)).loginWithFirebase(request);
        assertEquals(expectedResponse, response.getBody());
    }
}