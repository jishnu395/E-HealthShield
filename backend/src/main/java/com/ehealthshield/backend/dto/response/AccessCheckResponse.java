package com.ehealthshield.backend.dto.response;

import java.util.UUID;

public record AccessCheckResponse(
        UUID recordId,
        String doctorWallet,
        boolean hasAccess
) {
}
