package com.ehealthshield.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe metadata response for a search result.
 * Never contains plaintext EHR content, AES keys, ML-KEM keys, or plaintext keywords.
 */
public record EhrSearchResultResponse(
        UUID recordId,
        String fileName,
        String contentType,
        String patientWallet,
        String uploaderWallet,
        String fileHash,
        Instant createdAt
) {
}
