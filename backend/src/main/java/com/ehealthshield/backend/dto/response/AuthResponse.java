package com.ehealthshield.backend.dto.response;

import com.ehealthshield.backend.entity.UserRole;

public record AuthResponse(
        String token,
        String walletAddress,
        UserRole role,
        long expiresIn
) {
}
