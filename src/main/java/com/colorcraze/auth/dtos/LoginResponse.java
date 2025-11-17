package com.colorcraze.auth.dtos;

public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserData userData;

    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, UserData userData) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userData = userData;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserData getUserData() { return userData; }
    public void setUserData(UserData userData) { this.userData = userData; }
}
