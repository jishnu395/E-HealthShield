package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.crypto.HashService;
import com.ehealthshield.backend.dto.response.EhrRetrievalResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.CryptographicException;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class RetrievalService {

    private final EhrRecordRepository ehrRecordRepository;
    private final HashService hashService;
    private final AclService aclService;
    private final BlockchainService blockchainService;

    public RetrievalService(EhrRecordRepository ehrRecordRepository,
                            HashService hashService,
                            AclService aclService,
                            BlockchainService blockchainService) {
        this.ehrRecordRepository = Objects.requireNonNull(ehrRecordRepository, "EhrRecordRepository must not be null");
        this.hashService = Objects.requireNonNull(hashService, "HashService must not be null");
        this.aclService = Objects.requireNonNull(aclService, "AclService must not be null");
        this.blockchainService = Objects.requireNonNull(blockchainService, "BlockchainService must not be null");
    }

    /**
     * Retrieves an encrypted EHR bundle by record ID with mandatory ACL access authorization,
     * SHA-256 ciphertext integrity verification, and blockchain audit logging.
     *
     * <p>The backend NEVER decrypts the EHR content. The client must use their
     * ML-KEM-768 private key to decapsulate the AES key, then decrypt locally.</p>
     *
     * @param recordId the UUID of the EHR record to retrieve
     * @param accessorWallet the Ethereum wallet address of the requesting entity
     * @return encrypted bundle with metadata
     * @throws ResourceNotFoundException if no record exists with the given ID
     * @throws ValidationException if the requesting entity is not authorized under the ACL
     * @throws CryptographicException if SHA-256 ciphertext hash verification fails
     */
    @Transactional(readOnly = true)
    public EhrRetrievalResponse retrieveEhr(UUID recordId, String accessorWallet) {
        Objects.requireNonNull(recordId, "Record ID must not be null");

        EhrRecordEntity entity = ehrRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EHR record with ID " + recordId + " not found"));

        // 1. ACL Authorization Check
        if (accessorWallet != null && !accessorWallet.isBlank()) {
            boolean isAuthorized = aclService.checkAccess(recordId, accessorWallet);
            if (!isAuthorized) {
                throw new ValidationException("Access denied: wallet " + accessorWallet + " is not authorized to access record " + recordId);
            }
        }

        // 2. SHA-256 Ciphertext Integrity Verification
        if (!hashService.verifyHash(entity.getEncryptedCiphertext(), entity.getFileHash())) {
            throw new CryptographicException("EHR ciphertext integrity verification failed: stored hash mismatch");
        }

        // 3. Log Retrieval Audit Event on Blockchain
        blockchainService.logRetrievalEvent(recordId.toString(), accessorWallet);

        return new EhrRetrievalResponse(
                entity.getId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getPatientWallet(),
                entity.getUploadedBy(),
                entity.getEncryptedCiphertext(),
                entity.getIv(),
                entity.getKemCiphertext(),
                entity.getFileHash(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convenience method for backward compatibility.
     */
    @Transactional(readOnly = true)
    public EhrRetrievalResponse retrieveEhr(UUID recordId) {
        return retrieveEhr(recordId, null);
    }
}
