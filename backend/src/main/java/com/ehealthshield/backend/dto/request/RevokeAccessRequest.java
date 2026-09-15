package com.ehealthshield.backend.dto.request;

import java.util.UUID;

public record RevokeAccessRequest(
        UUID recordId,
        String doctorWallet
) {
}
