package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

@Service
public class AesCryptoService {

    public static final String ALGORITHM = "AES";
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int KEY_SIZE_BITS = 256;
    public static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits
    public static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure 256-bit AES key.
     */
    public byte[] generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE_BITS, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptographicException("Failed to generate AES-256 key", e);
        }
    }

    /**
     * Generates a unique, cryptographically secure 96-bit IV.
     */
    public byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a newly generated 256-bit key and unique 96-bit IV.
     */
    public AesEncryptionResult encrypt(byte[] plaintext) {
        Objects.requireNonNull(plaintext, "Plaintext must not be null");
        byte[] key = generateKey();
        byte[] iv = generateIv();
        byte[] ciphertext = encryptWithKeyAndIv(plaintext, key, iv);
        return new AesEncryptionResult(ciphertext, key, iv);
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a provided key and newly generated 96-bit IV.
     */
    public AesEncryptionResult encrypt(byte[] plaintext, byte[] key) {
        Objects.requireNonNull(plaintext, "Plaintext must not be null");
        Objects.requireNonNull(key, "AES key must not be null");
        if (key.length != KEY_SIZE_BITS / 8) {
            throw new CryptographicException("Invalid key size: expected 32 bytes (256 bits), got " + key.length);
        }
        byte[] iv = generateIv();
        byte[] ciphertext = encryptWithKeyAndIv(plaintext, key, iv);
        return new AesEncryptionResult(ciphertext, key, iv);
    }

    /**
     * Decrypts ciphertext using AES-256-GCM given the ciphertext, 256-bit key, and 96-bit IV.
     */
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) {
        Objects.requireNonNull(ciphertext, "Ciphertext must not be null");
        Objects.requireNonNull(key, "AES key must not be null");
        Objects.requireNonNull(iv, "IV must not be null");

        if (key.length != KEY_SIZE_BITS / 8) {
            throw new CryptographicException("Invalid key size: expected 32 bytes (256 bits), got " + key.length);
        }
        if (iv.length != GCM_IV_LENGTH_BYTES) {
            throw new CryptographicException("Invalid IV size: expected 12 bytes (96 bits), got " + iv.length);
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new CryptographicException("AES-256-GCM decryption failed: authentication tag mismatch or corrupted data", e);
        }
    }

    private byte[] encryptWithKeyAndIv(byte[] plaintext, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new CryptographicException("AES-256-GCM encryption failed", e);
        }
    }
}
