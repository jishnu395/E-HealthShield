package com.ehealthshield.backend.service;

import com.ehealthshield.backend.crypto.SseService;
import com.ehealthshield.backend.dto.response.EhrSearchResultResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import com.ehealthshield.backend.repository.RecordSearchTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private RecordSearchTagRepository searchTagRepository;

    @Mock
    private EhrRecordRepository ehrRecordRepository;

    @Mock
    private com.ehealthshield.backend.blockchain.BlockchainService blockchainService;

    private SseService sseService;
    private SearchService searchService;

    private static final String SSE_KEY = "test_master_sse_key_secret_for_unit_tests_123456";
    private static final String PATIENT_WALLET = "0x1111111111111111111111111111111111111111";
    private static final String DOCTOR_WALLET = "0x2222222222222222222222222222222222222222";

    private UUID recordId1;
    private UUID recordId2;
    private EhrRecordEntity record1;
    private EhrRecordEntity record2;

    @BeforeEach
    void setUp() {
        sseService = new SseService(SSE_KEY);
        searchService = new SearchService(sseService, searchTagRepository, ehrRecordRepository, blockchainService);

        recordId1 = UUID.randomUUID();
        recordId2 = UUID.randomUUID();

        record1 = EhrRecordEntity.builder()
                .id(recordId1)
                .patientWallet(PATIENT_WALLET)
                .uploadedBy(DOCTOR_WALLET)
                .fileName("blood_test.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(new byte[]{1, 2, 3})
                .kemCiphertext(new byte[1088])
                .iv(new byte[12])
                .fileHash("aabbccdd" + "00".repeat(28))
                .createdAt(Instant.now())
                .build();

        record2 = EhrRecordEntity.builder()
                .id(recordId2)
                .patientWallet(PATIENT_WALLET)
                .uploadedBy(DOCTOR_WALLET)
                .fileName("ecg_report.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(new byte[]{4, 5, 6})
                .kemCiphertext(new byte[1088])
                .iv(new byte[12])
                .fileHash("eeff0011" + "00".repeat(28))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Matching keyword returns correct record metadata")
    void testMatchingKeywordReturnsRecords() {
        String keyword = "cardiology";
        String trapdoor = sseService.generateTrapdoor(keyword);

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoor))
                .thenReturn(List.of(recordId1));
        when(ehrRecordRepository.findAllById(List.of(recordId1)))
                .thenReturn(List.of(record1));

        List<EhrSearchResultResponse> results = searchService.searchByKeyword(keyword);

        assertEquals(1, results.size());
        EhrSearchResultResponse result = results.get(0);
        assertEquals(recordId1, result.recordId());
        assertEquals("blood_test.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        assertEquals(PATIENT_WALLET, result.patientWallet());
        assertEquals(DOCTOR_WALLET, result.uploaderWallet());
        assertNotNull(result.fileHash());
        assertNotNull(result.createdAt());
    }

    @Test
    @DisplayName("2. Non-matching keyword returns empty list")
    void testNonMatchingKeywordReturnsEmptyList() {
        String keyword = "nonexistentkeyword";
        String trapdoor = sseService.generateTrapdoor(keyword);

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoor))
                .thenReturn(List.of());

        List<EhrSearchResultResponse> results = searchService.searchByKeyword(keyword);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("3. Case normalization: 'CARDIOLOGY' matches same tag as 'cardiology'")
    void testCaseNormalization() {
        String trapdoorLower = sseService.generateTrapdoor("cardiology");
        String trapdoorUpper = sseService.generateTrapdoor("CARDIOLOGY");

        assertEquals(trapdoorLower, trapdoorUpper, "Trapdoors for same keyword with different case must be identical");

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoorLower))
                .thenReturn(List.of(recordId1));
        when(ehrRecordRepository.findAllById(List.of(recordId1)))
                .thenReturn(List.of(record1));

        List<EhrSearchResultResponse> resultsLower = searchService.searchByKeyword("cardiology");
        List<EhrSearchResultResponse> resultsUpper = searchService.searchByKeyword("CARDIOLOGY");

        assertEquals(resultsLower.size(), resultsUpper.size());
        assertEquals(resultsLower.get(0).recordId(), resultsUpper.get(0).recordId());
    }

    @Test
    @DisplayName("4. Whitespace normalization: '  cardiology  ' matches 'cardiology'")
    void testWhitespaceNormalization() {
        String trapdoorTrimmed = sseService.generateTrapdoor("cardiology");
        String trapdoorWhitespace = sseService.generateTrapdoor("  cardiology  ");

        assertEquals(trapdoorTrimmed, trapdoorWhitespace, "Trapdoors must be identical after whitespace normalization");

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoorTrimmed))
                .thenReturn(List.of(recordId1));
        when(ehrRecordRepository.findAllById(List.of(recordId1)))
                .thenReturn(List.of(record1));

        List<EhrSearchResultResponse> results = searchService.searchByKeyword("  cardiology  ");

        assertEquals(1, results.size());
        assertEquals(recordId1, results.get(0).recordId());
    }

    @Test
    @DisplayName("5. Multiple matching records are all returned")
    void testMultipleMatchingRecords() {
        String keyword = "ecg";
        String trapdoor = sseService.generateTrapdoor(keyword);

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoor))
                .thenReturn(List.of(recordId1, recordId2));
        when(ehrRecordRepository.findAllById(List.of(recordId1, recordId2)))
                .thenReturn(List.of(record1, record2));

        List<EhrSearchResultResponse> results = searchService.searchByKeyword(keyword);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("6. Blank keyword throws ValidationException")
    void testBlankKeywordThrowsValidation() {
        assertThrows(ValidationException.class, () -> searchService.searchByKeyword(""));
        assertThrows(ValidationException.class, () -> searchService.searchByKeyword("   "));
        assertThrows(ValidationException.class, () -> searchService.searchByKeyword(null));
    }

    @Test
    @DisplayName("7. Search result never contains plaintext EHR content or cryptographic keys")
    void testSearchResultZeroKnowledge() {
        String keyword = "glucose";
        String trapdoor = sseService.generateTrapdoor(keyword);

        when(searchTagRepository.findRecordIdsBySearchTag(trapdoor))
                .thenReturn(List.of(recordId1));
        when(ehrRecordRepository.findAllById(List.of(recordId1)))
                .thenReturn(List.of(record1));

        List<EhrSearchResultResponse> results = searchService.searchByKeyword(keyword);
        EhrSearchResultResponse result = results.get(0);

        // Verify the DTO fields — only safe metadata
        assertNotNull(result.recordId());
        assertNotNull(result.fileName());
        assertNotNull(result.contentType());
        assertNotNull(result.patientWallet());
        assertNotNull(result.uploaderWallet());
        assertNotNull(result.fileHash());
        assertNotNull(result.createdAt());
        // The record type has no fields for encryptedCiphertext, kemCiphertext, iv, aesKey, etc.
    }

    @Test
    @DisplayName("8. Plaintext keyword is never stored — only HMAC trapdoor is used for lookup")
    void testPlaintextKeywordNeverPersisted() {
        String keyword = "penicillin";
        String trapdoor = sseService.generateTrapdoor(keyword);

        // Verify the trapdoor is a 64-char hex HMAC, not the plaintext keyword
        assertEquals(64, trapdoor.length());
        assertFalse(trapdoor.contains("penicillin"), "Trapdoor must never contain the plaintext keyword");

        // The service only ever passes the trapdoor to the repository, not the keyword
        when(searchTagRepository.findRecordIdsBySearchTag(trapdoor))
                .thenReturn(List.of());

        searchService.searchByKeyword(keyword);
        // If the plaintext keyword were passed, findRecordIdsBySearchTag("penicillin") would match
        // but we only set up the mock for the trapdoor — confirming correct behavior
    }
}
