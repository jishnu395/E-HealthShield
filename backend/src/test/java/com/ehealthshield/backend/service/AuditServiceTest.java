package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.blockchain.OnChainRecordMetadata;
import com.ehealthshield.backend.crypto.HashService;
import com.ehealthshield.backend.dto.response.AuditVerificationResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private EhrRecordRepository ehrRecordRepository;

    @Mock
    private BlockchainService blockchainService;

    private HashService hashService;
    private AuditService auditService;

    private UUID recordId;
    private byte[] ciphertext;
    private String fileHash;
    private EhrRecordEntity record;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
        auditService = new AuditService(ehrRecordRepository, blockchainService, hashService);

        recordId = UUID.randomUUID();
        ciphertext = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        fileHash = hashService.hash(ciphertext);

        record = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet("0x70997970C51812dc3A010C7d01b50e0d17dc79C8")
                .uploadedBy("0x90F79bf6EB2c4f870365E785982E1f101E93b906")
                .fileName("mri.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(ciphertext)
                .kemCiphertext(new byte[1088])
                .iv(new byte[12])
                .fileHash(fileHash)
                .blockchainTransactionHash("0x" + "aa".repeat(32))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Integrity verification confirms tamper-free when local and on-chain hashes match")
    void testVerifyRecordIntegrityTamperFree() {
        OnChainRecordMetadata onChainMeta = new OnChainRecordMetadata(
                fileHash,
                "0x70997970c51812dc3a010c7d01b50e0d17dc79c8",
                "0x90f79bf6eb2c4f870365e785982e1f101e93b906",
                1700000000L,
                true
        );

        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(blockchainService.getRecordMetadata(recordId.toString())).thenReturn(onChainMeta);

        AuditVerificationResponse response = auditService.verifyRecordIntegrity(recordId);

        assertNotNull(response);
        assertEquals(recordId, response.recordId());
        assertTrue(response.isTamperFree());
        assertEquals(fileHash, response.localCiphertextHash());
        assertEquals(fileHash, response.onChainAnchoredHash());
    }

    @Test
    @DisplayName("2. Integrity verification detects tampering if on-chain hash differs from database ciphertext")
    void testVerifyRecordIntegrityTamperedFails() {
        OnChainRecordMetadata tamperedOnChain = new OnChainRecordMetadata(
                "00".repeat(32), // Different hash
                "0x70997970c51812dc3a010c7d01b50e0d17dc79c8",
                "0x90f79bf6eb2c4f870365e785982e1f101e93b906",
                1700000000L,
                true
        );

        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(blockchainService.getRecordMetadata(recordId.toString())).thenReturn(tamperedOnChain);

        AuditVerificationResponse response = auditService.verifyRecordIntegrity(recordId);

        assertNotNull(response);
        assertFalse(response.isTamperFree(), "Mismatched on-chain hash must be flagged as tampered");
    }

    @Test
    @DisplayName("3. Nonexistent record throws ResourceNotFoundException")
    void testNonexistentRecordThrowsException() {
        UUID missingId = UUID.randomUUID();
        when(ehrRecordRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> auditService.verifyRecordIntegrity(missingId));
    }
}
