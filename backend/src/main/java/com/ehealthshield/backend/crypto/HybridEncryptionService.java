package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class HybridEncryptionService {

    private final AesCryptoService aesCryptoService;
    private final MlKemCryptoService mlKemCryptoService;
    private final HashService hashService;

    public HybridEncryptionService(AesCryptoService aesCryptoService,
                                   MlKemCryptoService mlKemCryptoService,
                                   HashService hashService) {
        this.aesCryptoService = Objects.requireNonNull(aesCryptoService, "AesCryptoService must not be null");
        this.mlKemCryptoService = Objects.requireNonNull(mlKemCryptoService, "MlKemCryptoService must not be null");
        this.hashService = Objects.requireNonNull(hashService, "HashService must not be null");
    }

    /**
     * Executes the hybrid encryption pipeline:
     * 1. Encapsulates a fresh 32-byte shared secret (AES-256 key) using patient's ML-KEM-768 public key.
     * 2. Encrypts the raw EHR document bytes using AES-256-GCM with a unique 96-bit IV.
     * 3. Computes the SHA-256 digest strictly over the resulting raw AES-GCM ciphertext bytes.
     * 4. Returns an immutable HybridEncryptionResult bundle.
     *
     * @param ehrPlaintext The raw EHR document bytes to protect.
     * @param patientMlKemPublicKey The recipient patient's 1184-byte ML-KEM-768 public key.
     * @return HybridEncryptionResult containing ciphertext, IV, ML-KEM ciphertext, derived AES key, and file hash.
     */
    public HybridEncryptionResult encrypt(byte[] ehrPlaintext, byte[] patientMlKemPublicKey) {
        Objects.requireNonNull(ehrPlaintext, "EHR plaintext must not be null");
        Objects.requireNonNull(patientMlKemPublicKey, "Patient ML-KEM public key must not be null");

        // 1. Quantum-safe key encapsulation (ML-KEM shared secret serves as the 32-byte AES key)
        MlKemEncapsulationResult kemResult = mlKemCryptoService.encapsulate(patientMlKemPublicKey);
        byte[] aesKey = kemResult.sharedSecret();
        byte[] kemCiphertext = kemResult.ciphertext();

        // 2. Symmetric payload encryption using AES-256-GCM
        AesEncryptionResult aesResult = aesCryptoService.encrypt(ehrPlaintext, aesKey);
        byte[] encryptedCiphertext = aesResult.ciphertext();
        byte[] iv = aesResult.iv();

        // 3. SHA-256 digest strictly over the raw AES-GCM ciphertext bytes
        String fileHash = hashService.hash(encryptedCiphertext);

        return new HybridEncryptionResult(
                encryptedCiphertext,
                iv,
                kemCiphertext,
                aesKey,
                fileHash
        );
    }

    /**
     * Executes the hybrid decryption pipeline:
     * 1. Decapsulates the AES-256 key using the patient's ML-KEM-768 private key.
     * 2. Decrypts the ciphertext using AES-256-GCM and verified IV.
     *
     * @param encryptedCiphertext The raw AES-GCM ciphertext bytes.
     * @param iv The 12-byte AES initialization vector.
     * @param kemCiphertext The 1088-byte ML-KEM-768 encapsulated key bundle.
     * @param patientMlKemPrivateKey The patient's 2400-byte ML-KEM-768 private key.
     * @return The original decrypted EHR plaintext bytes.
     */
    public byte[] decrypt(byte[] encryptedCiphertext, byte[] iv, byte[] kemCiphertext, byte[] patientMlKemPrivateKey) {
        Objects.requireNonNull(encryptedCiphertext, "Encrypted ciphertext must not be null");
        Objects.requireNonNull(iv, "IV must not be null");
        Objects.requireNonNull(kemCiphertext, "KEM ciphertext must not be null");
        Objects.requireNonNull(patientMlKemPrivateKey, "Patient ML-KEM private key must not be null");

        // 1. Decapsulate the shared secret (AES key) from KEM ciphertext
        byte[] aesKey = mlKemCryptoService.decapsulate(patientMlKemPrivateKey, kemCiphertext);

        // 2. Decrypt AES-256-GCM ciphertext using decapsulated key and IV
        return aesCryptoService.decrypt(encryptedCiphertext, aesKey, iv);
    }

    /**
     * Executes the hybrid decryption pipeline with prior SHA-256 integrity check:
     * 1. Verifies SHA-256 ciphertext hash against expected hash before attempting decryption.
     * 2. Decapsulates the AES-256 key using the patient's ML-KEM-768 private key.
     * 3. Decrypts the ciphertext using AES-256-GCM and verified IV.
     *
     * @param encryptedCiphertext The raw AES-GCM ciphertext bytes.
     * @param iv The 12-byte AES initialization vector.
     * @param kemCiphertext The 1088-byte ML-KEM-768 encapsulated key bundle.
     * @param patientMlKemPrivateKey The patient's 2400-byte ML-KEM-768 private key.
     * @param expectedFileHash The expected SHA-256 hex digest.
     * @return The original decrypted EHR plaintext bytes.
     * @throws CryptographicException if integrity verification or decryption fails.
     */
    public byte[] verifyAndDecrypt(byte[] encryptedCiphertext, byte[] iv, byte[] kemCiphertext,
                                   byte[] patientMlKemPrivateKey, String expectedFileHash) {
        Objects.requireNonNull(expectedFileHash, "Expected file hash must not be null");
        if (!hashService.verifyHash(encryptedCiphertext, expectedFileHash)) {
            throw new CryptographicException("EHR ciphertext integrity verification failed: computed hash does not match expected file hash");
        }
        return decrypt(encryptedCiphertext, iv, kemCiphertext, patientMlKemPrivateKey);
    }
}
