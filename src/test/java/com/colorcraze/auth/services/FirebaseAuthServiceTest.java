package com.colorcraze.auth.services;

import com.colorcraze.auth.dtos.FirebaseLoginRequest;
import com.colorcraze.auth.dtos.LoginResponse;
import com.colorcraze.auth.dtos.UserData;
import com.colorcraze.auth.models.AuthUser;
import com.colorcraze.auth.repositories.AuthUserRepository;
import com.colorcraze.utils.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthUserRepository userRepository;

    @Mock
    private FirebaseAuth firebaseAuth;

    @InjectMocks
    private FirebaseAuthService firebaseAuthService;

    @Test
    void loginWithFirebase_NewUser_Success() throws FirebaseAuthException {
        String uid = "test-uid";
        String email = "test@example.com";
        String name = "Test User";
        String jwt = "test-jwt";
        String refreshToken = UUID.randomUUID().toString();
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        new AuthUser(uid, email, name, "USER", refreshToken);
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(email);
        when(firebaseToken.getName()).thenReturn(name);
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(userRepository.findById(uid)).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(uid, "USER")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().isEmpty());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(uid, userData.getId());
        assertEquals(email, userData.getEmail());
        assertEquals(name, userData.getDisplayName());
        assertEquals("USER", userData.getRole());
        
        verify(userRepository).save(any(AuthUser.class));
        verify(userRepository).findById(uid);
    }

    @Test
    void loginWithFirebase_ExistingUser_Success() throws FirebaseAuthException {
        String uid = "existing-uid";
        String email = "existing@example.com";
        String name = "Existing User";
        String jwt = "test-jwt";
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        AuthUser existingUser = new AuthUser(uid, "old@example.com", "Old Name", "USER", "old-refresh");
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(email);
        when(firebaseToken.getName()).thenReturn(name);
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(userRepository.findById(uid)).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(uid, "USER")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().isEmpty());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(uid, userData.getId());
        assertEquals(email, userData.getEmail());
        assertEquals(name, userData.getDisplayName());
        
        verify(userRepository).save(existingUser);
        verify(userRepository).findById(uid);
    }

    @Test
    void loginWithFirebase_NullEmail_CreatesGuestUser() throws FirebaseAuthException {
        String uid = "test-uid";
        String jwt = "test-jwt";
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(null);
        when(firebaseToken.getName()).thenReturn("Test User");
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(jwtUtil.generateToken(uid, "GUEST")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNull(response.getRefreshToken());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(uid, userData.getId());
        assertEquals("GUEST", userData.getRole());
        assertTrue(userData.getEmail().matches("guest\\d{4}@colorcraze\\.com"));
        assertTrue(userData.getDisplayName().matches("Guest \\d{4}"));
        
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(AuthUser.class));
    }

    @Test
    void loginWithFirebase_NullName_CreatesGuestUser() throws FirebaseAuthException {
        String uid = "test-uid";
        String email = "test@example.com";
        String jwt = "test-jwt";
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(email);
        when(firebaseToken.getName()).thenReturn(null);
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(jwtUtil.generateToken(uid, "GUEST")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNull(response.getRefreshToken());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(uid, userData.getId());
        assertEquals("GUEST", userData.getRole());
        
        assertTrue(userData.getEmail().matches("guest\\d{4}@colorcraze\\.com"));
        assertTrue(userData.getDisplayName().matches("Guest \\d{4}"));
        
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(AuthUser.class));
    }

    @Test
    void loginWithFirebase_BlankEmail_CreatesGuestUser() throws FirebaseAuthException {
        String uid = "test-uid";
        String jwt = "test-jwt";
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn("");
        when(firebaseToken.getName()).thenReturn("Test User");
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(jwtUtil.generateToken(uid, "GUEST")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNull(response.getRefreshToken());
        assertEquals("GUEST", response.getUserData().getRole());
        
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(AuthUser.class));
    }

    @Test
    void loginWithFirebase_BlankName_CreatesGuestUser() throws FirebaseAuthException {
        String uid = "test-uid";
        String email = "test@example.com";
        String jwt = "test-jwt";
        
        FirebaseLoginRequest request = new FirebaseLoginRequest("firebase-id-token");
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        
        when(firebaseToken.getUid()).thenReturn(uid);
        when(firebaseToken.getEmail()).thenReturn(email);
        when(firebaseToken.getName()).thenReturn("");
        when(firebaseAuth.verifyIdToken(request.getIdToken())).thenReturn(firebaseToken);
        when(jwtUtil.generateToken(uid, "GUEST")).thenReturn(jwt);

        LoginResponse response = firebaseAuthService.loginWithFirebase(request);

        assertNotNull(response);
        assertEquals(jwt, response.getToken());
        assertNull(response.getRefreshToken());
        assertEquals("GUEST", response.getUserData().getRole());
        
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).save(any(AuthUser.class));
    }

    @Test
    void refresh_ValidToken_Success() {
        String refreshToken = "valid-refresh-token";
        String newJwt = "new-jwt-token";
        String uid = "user-uid";
        String email = "user@example.com";
        String name = "Test User";
        String role = "USER";
        
        AuthUser user = new AuthUser(uid, email, name, role, refreshToken);
        
        when(userRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(uid, role)).thenReturn(newJwt);

        LoginResponse response = firebaseAuthService.refresh(refreshToken);

        assertNotNull(response);
        assertEquals(newJwt, response.getToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getRefreshToken().isEmpty());
        assertNotEquals(refreshToken, response.getRefreshToken());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(uid, userData.getId());
        assertEquals(email, userData.getEmail());
        assertEquals(name, userData.getDisplayName());
        assertEquals(role, userData.getRole());
        
        verify(userRepository).save(user);
        verify(userRepository).findByRefreshToken(refreshToken);
    }

    @Test
    void refresh_InvalidToken_ThrowsIllegalArgumentException() {
        String invalidRefreshToken = "invalid-refresh-token";
        
        when(userRepository.findByRefreshToken(invalidRefreshToken)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> firebaseAuthService.refresh(invalidRefreshToken));

        assertEquals("Invalid refresh token", exception.getMessage());
        
        verify(userRepository, never()).save(any(AuthUser.class));
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void refresh_DifferentUserRole_UsesCorrectRole() {
        String refreshToken = "valid-refresh-token";
        String newJwt = "new-jwt-token";
        String uid = "admin-uid";
        String email = "admin@example.com";
        String name = "Admin User";
        String role = "ADMIN";
        
        AuthUser user = new AuthUser(uid, email, name, role, refreshToken);
        
        when(userRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(uid, role)).thenReturn(newJwt);

        LoginResponse response = firebaseAuthService.refresh(refreshToken);

        assertNotNull(response);
        assertEquals(newJwt, response.getToken());
        assertNotNull(response.getRefreshToken());
        
        UserData userData = response.getUserData();
        assertNotNull(userData);
        assertEquals(role, userData.getRole());
        
        verify(jwtUtil).generateToken(uid, role);
        verify(userRepository).save(user);
    }
}