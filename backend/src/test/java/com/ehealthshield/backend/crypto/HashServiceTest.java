package com.ehealthshield.backend.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashServiceTest {

    private HashService hashService;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
    }

    @Test
    @DisplayName("SHA-256 computes expected hash for known test vector (empty string and hello world)")
    void testSha256KnownValues() {
        // Known NIST SHA-256 for ""
        String emptyHash = hashService.hash("".getBytes(StandardCharsets.UTF_8));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", emptyHash);

        // Known NIST SHA-256 for "hello world"
        String helloWorldHash = hashService.hash("hello world".getBytes(StandardCharsets.UTF_8));
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", helloWorldHash);
    }

    @Test
    @DisplayName("SHA-256 produces 64-character lowercase hexadecimal string")
    void testHashFormat() {
        byte[] payload = "EHR medical report content for testing format".getBytes(StandardCharsets.UTF_8);
        String hash = hashService.hash(payload);

        assertEquals(64, hash.length(), "SHA-256 hex string must be 64 characters");
        assertEquals(hash.toLowerCase(), hash, "Hash must be in lowercase");
    }

    @Test
    @DisplayName("Constant-time hash comparison correctly validates matching and non-matching hashes")
    void testConstantTimeComparison() {
        byte[] data = "Ciphertext Payload".getBytes(StandardCharsets.UTF_8);
        String correctHash = hashService.hash(data);

        assertTrue(hashService.verifyHash(data, correctHash));
        assertTrue(hashService.constantTimeEquals(correctHash, correctHash));

        assertFalse(hashService.verifyHash(data, "0000000000000000000000000000000000000000000000000000000000000000"));
        assertFalse(hashService.constantTimeEquals(correctHash, "different_hash_string"));
    }
}
