package com.colorcraze.auth.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auth_users")
public class AuthUser {

    @Id
    private String id;

    private String uid;
    private String email;
    private String displayName;
    private String role;
    private String refreshToken;

    public AuthUser() {}

    public AuthUser(String uid, String email, String displayName, String role, String refreshToken) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.refreshToken = refreshToken;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
