package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.blockchain.OnChainRecordMetadata;
import com.ehealthshield.backend.crypto.HashService;
import com.ehealthshield.backend.dto.response.AuditVerificationResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class AuditService {

    private final EhrRecordRepository ehrRecordRepository;
    private final BlockchainService blockchainService;
    private final HashService hashService;

    public AuditService(EhrRecordRepository ehrRecordRepository,
                        BlockchainService blockchainService,
                        HashService hashService) {
        this.ehrRecordRepository = Objects.requireNonNull(ehrRecordRepository, "EhrRecordRepository must not be null");
        this.blockchainService = Objects.requireNonNull(blockchainService, "BlockchainService must not be null");
        this.hashService = Objects.requireNonNull(hashService, "HashService must not be null");
    }

    /**
     * Conducts end-to-end cryptographic integrity verification:
     * 1. Recomputes SHA-256 over raw PostgreSQL AES-GCM ciphertext bytes.
     * 2. Fetches immutable SHA-256 anchor hash from Ethereum smart contract state.
     * 3. Confirms non-tampering (recomputed hash == entity.fileHash == onChain.fileHash).
     */
    @Transactional(readOnly = true)
    public AuditVerificationResponse verifyRecordIntegrity(UUID recordId) {
        Objects.requireNonNull(recordId, "Record ID must not be null");

        EhrRecordEntity entity = ehrRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("EHR record with ID " + recordId + " not found"));

        String localRecomputedHash = hashService.hash(entity.getEncryptedCiphertext());
        OnChainRecordMetadata onChainRecord = blockchainService.getRecordMetadata(recordId.toString());

        boolean isTamperFree = onChainRecord.exists()
                && hashService.constantTimeEquals(localRecomputedHash, entity.getFileHash())
                && hashService.constantTimeEquals(localRecomputedHash, onChainRecord.fileHash());

        return new AuditVerificationResponse(
                entity.getId(),
                localRecomputedHash,
                onChainRecord.fileHash(),
                entity.getBlockchainTransactionHash(),
                isTamperFree,
                onChainRecord.timestamp()
        );
    }

    /**
     * Retrieves on-chain metadata for an EHR record directly from the smart contract state.
     */
    @Transactional(readOnly = true)
    public OnChainRecordMetadata getOnChainMetadata(UUID recordId) {
        Objects.requireNonNull(recordId, "Record ID must not be null");
        return blockchainService.getRecordMetadata(recordId.toString());
    }
}
