package com.ehealthshield.backend.dto.request;

import java.util.UUID;

public record GrantAccessRequest(
        UUID recordId,
        String doctorWallet
) {
}
