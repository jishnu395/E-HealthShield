package com.ehealthshield.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Encrypted EHR bundle for client-side decryption.
 * Contains ONLY ciphertexts and metadata — never plaintext content or symmetric keys.
 * The client must use their ML-KEM-768 private key to decapsulate the AES key and decrypt.
 */
public record EhrRetrievalResponse(
        UUID recordId,
        String fileName,
        String contentType,
        String patientWallet,
        String uploaderWallet,
        byte[] encryptedCiphertext,
        byte[] iv,
        byte[] kemCiphertext,
        String fileHash,
        Instant createdAt
) {
}
