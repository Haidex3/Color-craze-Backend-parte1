package com.colorcraze.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response object returned after a successful authentication process.
 * This DTO contains the access token, refresh token, and the authenticated user information.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserData userData;
}
