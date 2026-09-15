package com.ehealthshield.backend.crypto;

import java.util.Objects;

public record MlKemEncapsulationResult(
        byte[] ciphertext,
        byte[] sharedSecret
) {
    public MlKemEncapsulationResult {
        Objects.requireNonNull(ciphertext, "Ciphertext must not be null");
        Objects.requireNonNull(sharedSecret, "Shared secret must not be null");
    }

    @Override
    public String toString() {
        return "MlKemEncapsulationResult[ciphertextLength=" + (ciphertext != null ? ciphertext.length : 0)
                + ", sharedSecretLength=" + (sharedSecret != null ? sharedSecret.length : 0) + "]";
    }
}
