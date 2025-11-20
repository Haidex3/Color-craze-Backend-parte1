package com.colorcraze.auth.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MongoDB entity representing an authenticated user in the system.
 * Stores user profile information and the associated refresh token.
 */
@Document(collection = "auth_users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthUser {

    @Id
    private String id;
    private String email;
    private String displayName;
    private String role;
    private String refreshToken;
}