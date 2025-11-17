package com.colorcraze.auth.dtos;


public class UserData {
    private String id;
    private String uid;
    private String email;
    private String displayName;
    private String role;

    public UserData() {}

    public UserData(String id, String uid, String email, String displayName, String role) {
        this.id = id;
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
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
}
