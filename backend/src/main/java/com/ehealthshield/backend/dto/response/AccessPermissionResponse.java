package com.ehealthshield.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AccessPermissionResponse(
        Long id,
        UUID recordId,
        String patientWallet,
        String doctorWallet,
        boolean isGranted,
        String blockchainTxHash,
        Instant createdAt,
        Instant updatedAt
) {
    public AccessPermissionResponse(Long id, UUID recordId, String patientWallet, String doctorWallet, boolean isGranted, Instant createdAt, Instant updatedAt) {
        this(id, recordId, patientWallet, doctorWallet, isGranted, null, createdAt, updatedAt);
    }
}
