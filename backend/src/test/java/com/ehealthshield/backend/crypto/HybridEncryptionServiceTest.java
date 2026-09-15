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
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridEncryptionServiceTest {

    private AesCryptoService aesCryptoService;
    private MlKemCryptoService mlKemCryptoService;
    private HashService hashService;
    private HybridEncryptionService hybridEncryptionService;

    @BeforeEach
    void setUp() {
        aesCryptoService = new AesCryptoService();
        mlKemCryptoService = new MlKemCryptoService();
        hashService = new HashService();
        hybridEncryptionService = new HybridEncryptionService(aesCryptoService, mlKemCryptoService, hashService);
    }

    @Test
    @DisplayName("1. testHybridEncryptionProducesValidResult - Verify all components and size invariants")
    void testHybridEncryptionProducesValidResult() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] ehrPlaintext = "PATIENT MEDICAL RECORD\nDiagnosis: Hypertension\nPrescription: Lisinopril 10mg".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(ehrPlaintext, patientKeyPair.publicKey());

        assertNotNull(result);
        assertNotNull(result.encryptedCiphertext());
        assertTrue(result.encryptedCiphertext().length > 0, "Ciphertext must not be empty");

        assertNotNull(result.iv());
        assertEquals(12, result.iv().length, "IV must be exactly 12 bytes (96 bits)");

        assertNotNull(result.kemCiphertext());
        assertEquals(1088, result.kemCiphertext().length, "ML-KEM-768 ciphertext must be exactly 1088 bytes");

        assertNotNull(result.aesKey());
        assertEquals(32, result.aesKey().length, "Derived AES key must be exactly 32 bytes (256 bits)");

        assertNotNull(result.fileHash());
        assertEquals(64, result.fileHash().length(), "SHA-256 file hash must be 64 hexadecimal characters");
        assertEquals(result.fileHash().toLowerCase(), result.fileHash(), "File hash must be lowercase hex");
    }

    @Test
    @DisplayName("2. testHybridEncryptDecryptRoundTrip - Decapsulation recovers AES key and restores exact original plaintext")
    void testHybridEncryptDecryptRoundTrip() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] originalPlaintext = "CONFIDENTIAL CLINICAL REPORT: Patient blood work within normal parameters.".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(originalPlaintext, patientKeyPair.publicKey());

        byte[] decryptedPlaintext = hybridEncryptionService.decrypt(
                result.encryptedCiphertext(),
                result.iv(),
                result.kemCiphertext(),
                patientKeyPair.privateKey()
        );

        assertNotNull(decryptedPlaintext);
        assertArrayEquals(originalPlaintext, decryptedPlaintext, "Decrypted plaintext must match the original EHR document");
    }

    @Test
    @DisplayName("3. testDifferentEncryptionsProduceDifferentCiphertexts - Repeated encryptions generate distinct IVs, ciphertexts and KEM bundles")
    void testDifferentEncryptionsProduceDifferentCiphertexts() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] plaintext = "IDENTICAL EHR RECORD CONTENT".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result1 = hybridEncryptionService.encrypt(plaintext, patientKeyPair.publicKey());
        HybridEncryptionResult result2 = hybridEncryptionService.encrypt(plaintext, patientKeyPair.publicKey());

        assertFalse(Arrays.equals(result1.iv(), result2.iv()), "IVs must be unique across encryptions");
        assertFalse(Arrays.equals(result1.encryptedCiphertext(), result2.encryptedCiphertext()), "AES ciphertexts must differ");
        assertFalse(Arrays.equals(result1.kemCiphertext(), result2.kemCiphertext()), "ML-KEM ciphertexts must differ");
        assertFalse(Arrays.equals(result1.aesKey(), result2.aesKey()), "Derived AES keys must be unique ephemeral secrets");
        assertFalse(result1.fileHash().equals(result2.fileHash()), "Ciphertext hashes must differ due to unique ciphertexts");
    }

    @Test
    @DisplayName("4. testCiphertextHashMatches - SHA-256 hash strictly matches raw AES-GCM ciphertext digest")
    void testCiphertextHashMatches() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] plaintext = "Sample EHR Data For Hash Check".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(plaintext, patientKeyPair.publicKey());

        String expectedHash = hashService.hash(result.encryptedCiphertext());
        assertEquals(expectedHash, result.fileHash(), "result.fileHash must equal hashService.hash(encryptedCiphertext)");
        assertTrue(hashService.verifyHash(result.encryptedCiphertext(), result.fileHash()));
    }

    @Test
    @DisplayName("5. testTamperedCiphertextFailsIntegrityVerification - Bit flipping triggers hash verification failure")
    void testTamperedCiphertextFailsIntegrityVerification() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] plaintext = "EHR file payload".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(plaintext, patientKeyPair.publicKey());

        byte[] tamperedCiphertext = Arrays.copyOf(result.encryptedCiphertext(), result.encryptedCiphertext().length);
        tamperedCiphertext[0] ^= 0x01; // Flip single bit

        assertFalse(hashService.verifyHash(tamperedCiphertext, result.fileHash()),
                "Tampered ciphertext must fail SHA-256 integrity verification");
    }

    @Test
    @DisplayName("6. testTamperedCiphertextCannotBeDecrypted - Tampered ciphertext fails GCM authentication")
    void testTamperedCiphertextCannotBeDecrypted() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] plaintext = "Sensitive Medical History".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(plaintext, patientKeyPair.publicKey());

        byte[] tamperedCiphertext = Arrays.copyOf(result.encryptedCiphertext(), result.encryptedCiphertext().length);
        tamperedCiphertext[0] ^= 0x55;

        assertThrows(CryptographicException.class, () -> {
            hybridEncryptionService.decrypt(
                    tamperedCiphertext,
                    result.iv(),
                    result.kemCiphertext(),
                    patientKeyPair.privateKey()
            );
        }, "Decryption of tampered ciphertext must throw CryptographicException due to GCM auth tag mismatch");
    }

    @Test
    @DisplayName("7. testWrongPatientPrivateKeyCannotRecoverOriginalAESKey - Mismatched private key fails decryption")
    void testWrongPatientPrivateKeyCannotRecoverOriginalAESKey() {
        MlKemKeyPair intendedPatient = mlKemCryptoService.generateKeyPair();
        MlKemKeyPair unauthorizedParty = mlKemCryptoService.generateKeyPair();

        byte[] plaintext = "Confidential Diagnostic Notes".getBytes(StandardCharsets.UTF_8);
        HybridEncryptionResult result = hybridEncryptionService.encrypt(plaintext, intendedPatient.publicKey());

        // Attempt decryption with unauthorizedParty's private key
        assertThrows(CryptographicException.class, () -> {
            hybridEncryptionService.decrypt(
                    result.encryptedCiphertext(),
                    result.iv(),
                    result.kemCiphertext(),
                    unauthorizedParty.privateKey()
            );
        }, "Decryption using wrong patient private key must fail GCM tag authentication");
    }

    @Test
    @DisplayName("8. testVerifyAndDecryptSuccess - Successful decryption with verified SHA-256 hash")
    void testVerifyAndDecryptSuccess() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] originalPlaintext = "VERIFIED CLINICAL SUMMARY: Normal CBC panel.".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(originalPlaintext, patientKeyPair.publicKey());

        byte[] decrypted = hybridEncryptionService.verifyAndDecrypt(
                result.encryptedCiphertext(),
                result.iv(),
                result.kemCiphertext(),
                patientKeyPair.privateKey(),
                result.fileHash()
        );

        assertArrayEquals(originalPlaintext, decrypted);
    }

    @Test
    @DisplayName("9. testVerifyAndDecryptHashMismatchFails - Tampered hash or ciphertext aborts with CryptographicException")
    void testVerifyAndDecryptHashMismatchFails() {
        MlKemKeyPair patientKeyPair = mlKemCryptoService.generateKeyPair();
        byte[] originalPlaintext = "SENSITIVE MEDICAL RECORD".getBytes(StandardCharsets.UTF_8);

        HybridEncryptionResult result = hybridEncryptionService.encrypt(originalPlaintext, patientKeyPair.publicKey());

        // Attempt verifyAndDecrypt with wrong expected hash
        assertThrows(CryptographicException.class, () -> {
            hybridEncryptionService.verifyAndDecrypt(
                    result.encryptedCiphertext(),
                    result.iv(),
                    result.kemCiphertext(),
                    patientKeyPair.privateKey(),
                    "00".repeat(32) // wrong hash
            );
        }, "verifyAndDecrypt with mismatched hash must throw CryptographicException");
    }
}
