package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesCryptoServiceTest {

    private AesCryptoService aesCryptoService;

    @BeforeEach
    void setUp() {
        aesCryptoService = new AesCryptoService();
    }

    @Test
    @DisplayName("AES-256-GCM encrypt/decrypt round-trip succeeds")
    void testEncryptDecryptRoundTrip() {
        byte[] originalPlaintext = "Confidential Patient EHR Medical Record: Diagnosis Diabetes Type-2".getBytes(StandardCharsets.UTF_8);

        AesEncryptionResult result = aesCryptoService.encrypt(originalPlaintext);

        assertNotNull(result);
        assertNotNull(result.ciphertext());
        assertNotNull(result.key());
        assertNotNull(result.iv());
        assertEquals(32, result.key().length, "Key must be 256 bits (32 bytes)");
        assertEquals(12, result.iv().length, "IV must be 96 bits (12 bytes)");

        byte[] decryptedBytes = aesCryptoService.decrypt(result.ciphertext(), result.key(), result.iv());

        assertArrayEquals(originalPlaintext, decryptedBytes, "Decrypted text must match original plaintext");
    }

    @Test
    @DisplayName("Different encryptions of same plaintext produce unique IVs and ciphertexts")
    void testDifferentEncryptionProducesDifferentIvAndCiphertext() {
        byte[] plaintext = "Identical EHR Content".getBytes(StandardCharsets.UTF_8);

        AesEncryptionResult result1 = aesCryptoService.encrypt(plaintext);
        AesEncryptionResult result2 = aesCryptoService.encrypt(plaintext);

        assertFalse(Arrays.equals(result1.iv(), result2.iv()), "IVs must be uniquely generated for every encryption");
        assertFalse(Arrays.equals(result1.ciphertext(), result2.ciphertext()), "Ciphertexts must differ due to distinct IVs/keys");
    }

    @Test
    @DisplayName("Tampering with ciphertext causes GCM authentication tag verification failure")
    void testTamperingCausesAuthenticationFailure() {
        byte[] plaintext = "Sensitive EHR Payload".getBytes(StandardCharsets.UTF_8);
        AesEncryptionResult result = aesCryptoService.encrypt(plaintext);

        byte[] tamperedCiphertext = Arrays.copyOf(result.ciphertext(), result.ciphertext().length);
        tamperedCiphertext[0] ^= 0xFF; // Flip bits in the ciphertext

        assertThrows(CryptographicException.class, () -> {
            aesCryptoService.decrypt(tamperedCiphertext, result.key(), result.iv());
        }, "Tampering must trigger authentication tag mismatch exception");
    }

    @Test
    @DisplayName("Tampering with IV causes authentication failure")
    void testTamperingWithIvCausesFailure() {
        byte[] plaintext = "Patient Prescription Data".getBytes(StandardCharsets.UTF_8);
        AesEncryptionResult result = aesCryptoService.encrypt(plaintext);

        byte[] tamperedIv = Arrays.copyOf(result.iv(), result.iv().length);
        tamperedIv[0] ^= 0x01; // Modify IV

        assertThrows(CryptographicException.class, () -> {
            aesCryptoService.decrypt(result.ciphertext(), result.key(), tamperedIv);
        }, "Corrupted IV must fail decryption authentication");
    }
}
