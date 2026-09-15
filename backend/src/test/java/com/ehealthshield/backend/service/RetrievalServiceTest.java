package com.ehealthshield.backend.service;

import com.ehealthshield.backend.crypto.HashService;
import com.ehealthshield.backend.dto.response.EhrRetrievalResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.CryptographicException;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private EhrRecordRepository ehrRecordRepository;

    @Mock
    private AclService aclService;

    @Mock
    private com.ehealthshield.backend.blockchain.BlockchainService blockchainService;

    private HashService hashService;
    private RetrievalService retrievalService;

    private static final String PATIENT_WALLET = "0x1111111111111111111111111111111111111111";
    private static final String DOCTOR_WALLET = "0x2222222222222222222222222222222222222222";
    private static final String UNAUTHORIZED_WALLET = "0x9999999999999999999999999999999999999999";

    private UUID recordId;
    private byte[] encryptedCiphertext;
    private byte[] iv;
    private byte[] kemCiphertext;
    private String fileHash;
    private EhrRecordEntity record;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
        retrievalService = new RetrievalService(ehrRecordRepository, hashService, aclService, blockchainService);

        recordId = UUID.randomUUID();
        encryptedCiphertext = new byte[]{10, 20, 30, 40, 50, 60, 70, 80};
        iv = new byte[12];
        kemCiphertext = new byte[1088];

        for (int i = 0; i < iv.length; i++) iv[i] = (byte) (i + 1);
        for (int i = 0; i < kemCiphertext.length; i++) kemCiphertext[i] = (byte) (i % 256);

        fileHash = hashService.hash(encryptedCiphertext);

        record = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet(PATIENT_WALLET)
                .uploadedBy(DOCTOR_WALLET)
                .fileName("ecg_scan.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(encryptedCiphertext)
                .kemCiphertext(kemCiphertext)
                .iv(iv)
                .fileHash(fileHash)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Successful retrieval returns complete encrypted bundle")
    void testSuccessfulRetrieval() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        EhrRetrievalResponse response = retrievalService.retrieveEhr(recordId);

        assertNotNull(response);
        assertEquals(recordId, response.recordId());
        assertEquals("ecg_scan.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(PATIENT_WALLET, response.patientWallet());
        assertEquals(DOCTOR_WALLET, response.uploaderWallet());
        assertArrayEquals(encryptedCiphertext, response.encryptedCiphertext());
        assertArrayEquals(iv, response.iv());
        assertArrayEquals(kemCiphertext, response.kemCiphertext());
        assertEquals(fileHash, response.fileHash());
        assertNotNull(response.createdAt());
    }

    @Test
    @DisplayName("2. Successful retrieval with authorized wallet")
    void testSuccessfulRetrievalWithAuthorizedWallet() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(aclService.checkAccess(recordId, PATIENT_WALLET)).thenReturn(true);

        EhrRetrievalResponse response = retrievalService.retrieveEhr(recordId, PATIENT_WALLET);

        assertNotNull(response);
        assertEquals(recordId, response.recordId());
    }

    @Test
    @DisplayName("3. Unauthorized wallet retrieval is rejected with ValidationException")
    void testUnauthorizedWalletRetrievalRejected() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(aclService.checkAccess(recordId, UNAUTHORIZED_WALLET)).thenReturn(false);

        assertThrows(ValidationException.class, () ->
                retrievalService.retrieveEhr(recordId, UNAUTHORIZED_WALLET));
    }

    @Test
    @DisplayName("4. Nonexistent record ID throws ResourceNotFoundException")
    void testNonexistentRecordThrowsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(ehrRecordRepository.findById(missingId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> retrievalService.retrieveEhr(missingId));

        assertNotNull(ex.getMessage());
        assertEquals("EHR record with ID " + missingId + " not found", ex.getMessage());
    }

    @Test
    @DisplayName("5. Tampered ciphertext in DB (hash mismatch) throws CryptographicException")
    void testTamperedCiphertextThrowsIntegrityException() {
        EhrRecordEntity tamperedRecord = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet(PATIENT_WALLET)
                .uploadedBy(DOCTOR_WALLET)
                .fileName("tampered.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(new byte[]{99, 98, 97})
                .kemCiphertext(kemCiphertext)
                .iv(iv)
                .fileHash(fileHash)
                .createdAt(Instant.now())
                .build();

        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(tamperedRecord));

        assertThrows(CryptographicException.class, () -> retrievalService.retrieveEhr(recordId));
    }

    @Test
    @DisplayName("6. Response contains encrypted ciphertext — not decrypted plaintext")
    void testResponseIsEncryptedBundle() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        EhrRetrievalResponse response = retrievalService.retrieveEhr(recordId);

        assertArrayEquals(encryptedCiphertext, response.encryptedCiphertext());
        assertEquals(12, response.iv().length);
        assertEquals(1088, response.kemCiphertext().length);
        assertEquals(64, response.fileHash().length());
    }

    @Test
    @DisplayName("7. Null record ID throws NullPointerException")
    void testNullRecordIdThrowsException() {
        assertThrows(NullPointerException.class, () -> retrievalService.retrieveEhr(null));
    }
}
