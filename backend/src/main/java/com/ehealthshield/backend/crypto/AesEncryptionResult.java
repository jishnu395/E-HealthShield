package com.ehealthshield.backend.crypto;

public record AesEncryptionResult(
        byte[] ciphertext,
        byte[] key,
        byte[] iv
) {}
