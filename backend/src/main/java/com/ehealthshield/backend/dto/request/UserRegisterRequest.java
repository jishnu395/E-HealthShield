package com.ehealthshield.backend.dto.request;

import com.ehealthshield.backend.entity.UserRole;

public record UserRegisterRequest(
        String walletAddress,
        UserRole role,
        String kyberPublicKeyBase64
) {
}
