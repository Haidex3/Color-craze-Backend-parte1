package com.colorcraze.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents user information returned as part of an authentication response.
 * Contains core profile data such as identifiers, email, display name, and role.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserData {
    private String id;
    private String email;
    private String displayName;
    private String role;
}