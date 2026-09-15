package com.ehealthshield.backend.crypto;

import java.util.Objects;

public record HybridEncryptionResult(
        byte[] encryptedCiphertext,
        byte[] iv,
        byte[] kemCiphertext,
        byte[] aesKey,
        String fileHash
) {
    public HybridEncryptionResult {
        Objects.requireNonNull(encryptedCiphertext, "Encrypted ciphertext must not be null");
        Objects.requireNonNull(iv, "IV must not be null");
        Objects.requireNonNull(kemCiphertext, "KEM ciphertext must not be null");
        Objects.requireNonNull(aesKey, "AES key must not be null");
        Objects.requireNonNull(fileHash, "File hash must not be null");
    }

    @Override
    public String toString() {
        return "HybridEncryptionResult[ciphertextLength=" + (encryptedCiphertext != null ? encryptedCiphertext.length : 0)
                + ", ivLength=" + (iv != null ? iv.length : 0)
                + ", kemCiphertextLength=" + (kemCiphertext != null ? kemCiphertext.length : 0)
                + ", fileHash=" + fileHash + "]";
    }
}
