package com.ehealthshield.backend.crypto;

import com.ehealthshield.backend.exception.CryptographicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlKemNobleInteropTest {

    private MlKemCryptoService mlKemCryptoService;
    private AesCryptoService aesCryptoService;
    private HybridEncryptionService hybridEncryptionService;
    private HashService hashService;

    @BeforeEach
    void setUp() {
        mlKemCryptoService = new MlKemCryptoService();
        aesCryptoService = new AesCryptoService();
        hashService = new HashService();
        hybridEncryptionService = new HybridEncryptionService(aesCryptoService, mlKemCryptoService, hashService);
    }

    @Test
    @DisplayName("1. Java/Bouncy Castle Encapsulation -> ML-KEM-768 Decapsulation -> Identical 32-Byte AES Key & Plaintext Recovery")
    void testHybridEncryptionDecryptionPipeline() {
        // 1. Generate NIST FIPS 203 ML-KEM-768 Keypair
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();
        assertEquals(MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES, keyPair.publicKey().length);
        assertEquals(MlKemCryptoService.PRIVATE_KEY_SIZE_BYTES, keyPair.privateKey().length);

        // 2. Sample Clinical EHR Plaintext Document
        byte[] originalPlaintext = "Phase 5 Interoperability Report: Patient Blood Pressure 118/78 mmHg. Status: Healthy.".getBytes(StandardCharsets.UTF_8);

        // 3. Execute Hybrid Encryption (ML-KEM-768 Encapsulation + AES-256-GCM Encryption)
        HybridEncryptionResult hybridResult = hybridEncryptionService.encrypt(originalPlaintext, keyPair.publicKey());

        assertNotNull(hybridResult);
        assertEquals(MlKemCryptoService.CIPHERTEXT_SIZE_BYTES, hybridResult.kemCiphertext().length); // 1088 bytes
        assertEquals(32, hybridResult.aesKey().length); // 32-byte AES-256 key
        assertEquals(12, hybridResult.iv().length); // 12-byte GCM IV

        // 4. Verify SHA-256 Digest is computed over ciphertext
        String computedHash = hashService.hash(hybridResult.encryptedCiphertext());
        assertEquals(hybridResult.fileHash(), computedHash);

        // 5. Execute Decapsulation + Decryption
        byte[] decapsulatedKey = mlKemCryptoService.decapsulate(keyPair.privateKey(), hybridResult.kemCiphertext());
        assertArrayEquals(hybridResult.aesKey(), decapsulatedKey, "Decapsulated AES key must match encapsulated AES key byte-for-byte");

        byte[] recoveredPlaintext = hybridEncryptionService.verifyAndDecrypt(
                hybridResult.encryptedCiphertext(),
                hybridResult.iv(),
                hybridResult.kemCiphertext(),
                keyPair.privateKey(),
                hybridResult.fileHash()
        );

        // 6. Assert byte-for-byte equality
        assertArrayEquals(originalPlaintext, recoveredPlaintext);
        assertEquals(new String(originalPlaintext, StandardCharsets.UTF_8), new String(recoveredPlaintext, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("2. Tampered Ciphertext or Authentication Tag Rejection")
    void testTamperedCiphertextRejection() {
        MlKemKeyPair keyPair = mlKemCryptoService.generateKeyPair();
        byte[] originalPlaintext = "Confidential Patient Data".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult hybridResult = hybridEncryptionService.encrypt(originalPlaintext, keyPair.publicKey());

        // Tamper single byte in ciphertext
        byte[] tamperedCiphertext = Arrays.copyOf(hybridResult.encryptedCiphertext(), hybridResult.encryptedCiphertext().length);
        tamperedCiphertext[0] ^= (byte) 0xFF;

        // 1. Hash verification failure
        assertThrows(CryptographicException.class, () ->
                hybridEncryptionService.verifyAndDecrypt(
                        tamperedCiphertext,
                        hybridResult.iv(),
                        hybridResult.kemCiphertext(),
                        keyPair.privateKey(),
                        hybridResult.fileHash()
                )
        );

        // 2. AES-GCM Tag Mismatch failure
        assertThrows(CryptographicException.class, () ->
                hybridEncryptionService.decrypt(
                        tamperedCiphertext,
                        hybridResult.iv(),
                        hybridResult.kemCiphertext(),
                        keyPair.privateKey()
                )
        );
    }
}
