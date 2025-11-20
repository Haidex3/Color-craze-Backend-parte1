package com.colorcraze.auth.dtos;

public record UserDetailsResponse(
        String id,
        String email,
        String nickname) {
}
