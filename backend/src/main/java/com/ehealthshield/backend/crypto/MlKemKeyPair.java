package com.ehealthshield.backend.crypto;

import java.util.Objects;

public record MlKemKeyPair(
        byte[] publicKey,
        byte[] privateKey
) {
    public MlKemKeyPair {
        Objects.requireNonNull(publicKey, "Public key must not be null");
        Objects.requireNonNull(privateKey, "Private key must not be null");
    }

    @Override
    public String toString() {
        return "MlKemKeyPair[publicKeyLength=" + (publicKey != null ? publicKey.length : 0)
                + ", privateKeyLength=" + (privateKey != null ? privateKey.length : 0) + "]";
    }
}
