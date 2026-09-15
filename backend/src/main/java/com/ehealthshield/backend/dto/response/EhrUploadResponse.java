package com.ehealthshield.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EhrUploadResponse(
        UUID recordId,
        String fileName,
        String contentType,
        String patientWallet,
        String fileHash,
        Instant createdAt
) {
}
