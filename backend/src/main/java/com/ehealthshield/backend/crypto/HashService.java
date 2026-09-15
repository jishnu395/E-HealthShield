package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class HashService {

    public static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Computes the SHA-256 digest of the given byte array and returns a lowercase hex string.
     */
    public String hash(byte[] data) {
        Objects.requireNonNull(data, "Data to hash must not be null");
        byte[] digest = hashBytes(data);
        return HexFormat.of().formatHex(digest);
    }

    /**
     * Computes the SHA-256 digest of the given string and returns a lowercase hex string.
     */
    public String hash(String text) {
        Objects.requireNonNull(text, "Text to hash must not be null");
        return hash(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the raw SHA-256 digest bytes.
     */
    public byte[] hashBytes(byte[] data) {
        Objects.requireNonNull(data, "Data to hash must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison between two byte arrays to prevent timing attacks.
     */
    public boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Constant-time comparison between two hex hash strings.
     */
    public boolean constantTimeEquals(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return Objects.equals(hash1, hash2);
        }
        byte[] bytes1 = hash1.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = hash2.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bytes1, bytes2);
    }

    /**
     * Verifies that the raw data produces the expected hexadecimal hash (constant-time).
     */
    public boolean verifyHash(byte[] data, String expectedHexHash) {
        if (data == null || expectedHexHash == null) {
            return false;
        }
        String computedHash = hash(data);
        return constantTimeEquals(computedHash, expectedHexHash);
    }
}
