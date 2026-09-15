package com.ehealthshield.backend.dto.request;

public record AuthVerifyRequest(
        String walletAddress,
        String signature
) {
}
