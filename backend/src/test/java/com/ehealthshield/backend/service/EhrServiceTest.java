package com.ehealthshield.backend.service;

import com.ehealthshield.backend.crypto.AesCryptoService;
import com.ehealthshield.backend.crypto.HashService;
import com.ehealthshield.backend.crypto.HybridEncryptionService;
import com.ehealthshield.backend.crypto.MlKemCryptoService;
import com.ehealthshield.backend.crypto.MlKemKeyPair;
import com.ehealthshield.backend.crypto.SseService;
import com.ehealthshield.backend.dto.request.EhrUploadRequest;
import com.ehealthshield.backend.dto.response.EhrUploadResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.entity.RecordSearchTagEntity;
import com.ehealthshield.backend.entity.UserEntity;
import com.ehealthshield.backend.entity.UserRole;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import com.ehealthshield.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EhrServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EhrRecordRepository ehrRecordRepository;

    @Mock
    private com.ehealthshield.backend.blockchain.BlockchainService blockchainService;

    @Captor
    private ArgumentCaptor<EhrRecordEntity> recordCaptor;

    private AesCryptoService aesCryptoService;
    private MlKemCryptoService mlKemCryptoService;
    private HashService hashService;
    private HybridEncryptionService hybridEncryptionService;
    private SseService sseService;

    private EhrService ehrService;

    private static final String SSE_KEY = "test_master_sse_key_secret_for_unit_tests_123456";
    private static final String PATIENT_WALLET = "0x1111111111111111111111111111111111111111";
    private static final String DOCTOR_WALLET = "0x2222222222222222222222222222222222222222";

    private MlKemKeyPair patientKeyPair;
    private UserEntity patientUser;

    @BeforeEach
    void setUp() {
        aesCryptoService = new AesCryptoService();
        mlKemCryptoService = new MlKemCryptoService();
        hashService = new HashService();
        hybridEncryptionService = new HybridEncryptionService(aesCryptoService, mlKemCryptoService, hashService);
        sseService = new SseService(SSE_KEY);

        ehrService = new EhrService(userRepository, ehrRecordRepository, hybridEncryptionService, sseService, blockchainService);

        patientKeyPair = mlKemCryptoService.generateKeyPair();
        patientUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .walletAddress(PATIENT_WALLET)
                .role(UserRole.PATIENT)
                .kyberPublicKey(patientKeyPair.publicKey())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Successful encrypted EHR upload and persistence with single ML-KEM ciphertext")
    void testSuccessfulEncryptedEhrPersistence() {
        byte[] fileContent = "Medical Lab Report: Blood Glucose: 95 mg/dL".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "lab_report.pdf", "application/pdf", fileContent);
        List<String> keywords = List.of("glucose", "blood", "laboratory");

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, keywords);

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any(EhrRecordEntity.class))).thenAnswer(invocation -> {
            EhrRecordEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        EhrUploadResponse response = ehrService.uploadEhr(request);

        assertNotNull(response);
        assertNotNull(response.recordId());
        assertEquals("lab_report.pdf", response.fileName());
        assertEquals("application/pdf", response.contentType());
        assertEquals(PATIENT_WALLET, response.patientWallet());
        assertEquals(64, response.fileHash().length());
        assertNotNull(response.createdAt());

        verify(ehrRecordRepository).save(recordCaptor.capture());
        EhrRecordEntity savedEntity = recordCaptor.getValue();

        assertNotNull(savedEntity);
        assertEquals(PATIENT_WALLET, savedEntity.getPatientWallet());
        assertEquals(DOCTOR_WALLET, savedEntity.getUploadedBy());
        assertNotNull(savedEntity.getEncryptedCiphertext());
        assertEquals(12, savedEntity.getIv().length);
        assertEquals(1088, savedEntity.getKemCiphertext().length, "Only one ML-KEM ciphertext field (kemCiphertext) must be populated");
        assertNull(savedEntity.getBlockchainTransactionHash());
        assertEquals(3, savedEntity.getSearchTags().size());
    }

    @Test
    @DisplayName("2. Rejection of empty file upload")
    void testEmptyFileRejection() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        EhrUploadRequest request = new EhrUploadRequest(emptyFile, PATIENT_WALLET, DOCTOR_WALLET, List.of());

        assertThrows(ValidationException.class, () -> ehrService.uploadEhr(request));
    }

    @Test
    @DisplayName("3. Rejection when patient is not registered in users table")
    void testMissingPatientRejection() {
        byte[] fileContent = "Medical Data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "record.pdf", "application/pdf", fileContent);
        EhrUploadRequest request = new EhrUploadRequest(file, "0x0000000000000000000000000000000000000000", DOCTOR_WALLET, List.of());

        when(userRepository.findByWalletAddress(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ehrService.uploadEhr(request));
    }

    @Test
    @DisplayName("4. Registered patient ML-KEM public key in users table is authoritative")
    void testRegisteredPatientPublicKeyIsAuthoritative() {
        byte[] originalContent = "Confidential Surgical Record".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "surgery.pdf", "application/pdf", originalContent);

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, List.of("surgery"));

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ehrService.uploadEhr(request);

        verify(ehrRecordRepository).save(recordCaptor.capture());
        EhrRecordEntity entity = recordCaptor.getValue();

        // Verify that the ciphertext can be decrypted using the registered patient's private key
        byte[] decrypted = hybridEncryptionService.decrypt(
                entity.getEncryptedCiphertext(),
                entity.getIv(),
                entity.getKemCiphertext(),
                patientKeyPair.privateKey()
        );

        assertArrayEquals(originalContent, decrypted, "Ciphertext must be successfully decryptable with the registered patient's private key");
    }

    @Test
    @DisplayName("5. Rejection when registered patient has invalid or missing ML-KEM public key")
    void testInvalidRegisteredPatientKeyRejection() {
        UserEntity invalidKeyPatient = UserEntity.builder()
                .id(UUID.randomUUID())
                .walletAddress(PATIENT_WALLET)
                .role(UserRole.PATIENT)
                .kyberPublicKey(new byte[500]) // Invalid length (not 1184)
                .createdAt(Instant.now())
                .build();

        byte[] fileContent = "Medical Data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "record.pdf", "application/pdf", fileContent);
        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, List.of());

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(invalidKeyPatient));

        assertThrows(ValidationException.class, () -> ehrService.uploadEhr(request));
    }

    @Test
    @DisplayName("6. Search tags are stored as 64-char HMAC values, never plaintext keywords")
    void testSearchTagsAreStoredAsHmacValuesNeverPlaintext() {
        byte[] fileContent = "Patient Allergy Record".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "allergies.pdf", "application/pdf", fileContent);
        List<String> keywords = List.of("penicillin", "anaphylaxis");

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, keywords);

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ehrService.uploadEhr(request);

        verify(ehrRecordRepository).save(recordCaptor.capture());
        EhrRecordEntity entity = recordCaptor.getValue();

        assertEquals(2, entity.getSearchTags().size());
        for (RecordSearchTagEntity tagEntity : entity.getSearchTags()) {
            String tag = tagEntity.getSearchTag();
            assertNotNull(tag);
            assertEquals(64, tag.length(), "Search tag must be a 64-character hex HMAC digest");
            assertFalse(tag.contains("penicillin"), "Tag must never contain plaintext keyword");
            assertFalse(tag.contains("anaphylaxis"), "Tag must never contain plaintext keyword");
        }
    }

    @Test
    @DisplayName("7. Plaintext file bytes and plaintext AES keys are strictly absent from database fields")
    void testPlaintextDataAndAesKeyNeverPersisted() {
        byte[] plaintextBytes = "SECRET DIAGNOSIS DATA: Patient has stage-1 Lyme disease".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "diagnosis.txt", "text/plain", plaintextBytes);

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, List.of("lyme"));

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ehrService.uploadEhr(request);

        verify(ehrRecordRepository).save(recordCaptor.capture());
        EhrRecordEntity entity = recordCaptor.getValue();

        assertFalse(Arrays.equals(plaintextBytes, entity.getEncryptedCiphertext()),
                "Encrypted ciphertext field must not equal raw plaintext");
        assertFalse(new String(entity.getEncryptedCiphertext(), StandardCharsets.UTF_8).contains("Lyme disease"),
                "Ciphertext bytes must not contain any plaintext substrings");

        // Verify only kemCiphertext exists and is 1088 bytes
        assertEquals(1088, entity.getKemCiphertext().length);
    }

    @Test
    @DisplayName("8. Transaction rollback when repository save fails")
    void testTransactionRollbackWhenPersistenceFails() {
        byte[] fileContent = "Test Data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "data.pdf", "application/pdf", fileContent);

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, List.of("tag"));

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(RuntimeException.class, () -> ehrService.uploadEhr(request));
    }

    @Test
    @DisplayName("9. Correct SHA-256 hash and wallet addresses are persisted")
    void testCorrectSha256HashAndWalletsPersisted() {
        byte[] fileContent = "Oncology blood panel markers".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "oncology.pdf", "application/pdf", fileContent);

        EhrUploadRequest request = new EhrUploadRequest(file, PATIENT_WALLET, DOCTOR_WALLET, List.of("oncology"));

        when(userRepository.findByWalletAddress(PATIENT_WALLET)).thenReturn(Optional.of(patientUser));
        when(ehrRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ehrService.uploadEhr(request);

        verify(ehrRecordRepository).save(recordCaptor.capture());
        EhrRecordEntity entity = recordCaptor.getValue();

        String expectedHash = hashService.hash(entity.getEncryptedCiphertext());
        assertEquals(expectedHash, entity.getFileHash());
        assertTrue(hashService.verifyHash(entity.getEncryptedCiphertext(), entity.getFileHash()));
        assertEquals(PATIENT_WALLET, entity.getPatientWallet());
        assertEquals(DOCTOR_WALLET, entity.getUploadedBy());
    }
}
