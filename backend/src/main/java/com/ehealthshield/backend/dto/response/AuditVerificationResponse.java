package com.ehealthshield.backend.dto.response;

import java.util.UUID;

public record AuditVerificationResponse(
        UUID recordId,
        String localCiphertextHash,
        String onChainAnchoredHash,
        String blockchainTransactionHash,
        boolean isTamperFree,
        long onChainTimestamp
) {
}
