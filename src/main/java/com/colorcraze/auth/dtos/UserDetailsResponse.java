package com.colorcraze.auth.dtos;

/**
 * Response DTO containing basic public information about a user.
 * Used for delivering lightweight user profile details to the client.
 */
public record UserDetailsResponse(
        String id,
        String email,
        String nickname
) {
}