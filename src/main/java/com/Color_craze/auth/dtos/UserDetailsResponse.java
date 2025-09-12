package com.Color_craze.auth.dtos;

import java.util.UUID;

public record UserDetailsResponse(
        UUID id,
        String email,
        String name) {
}