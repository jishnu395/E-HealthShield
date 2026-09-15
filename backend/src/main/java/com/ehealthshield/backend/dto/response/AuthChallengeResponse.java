package com.ehealthshield.backend.dto.response;

public record AuthChallengeResponse(
        String walletAddress,
        String challenge
) {
}
