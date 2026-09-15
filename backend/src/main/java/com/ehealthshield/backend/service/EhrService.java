package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.crypto.HybridEncryptionResult;
import com.ehealthshield.backend.crypto.HybridEncryptionService;
import com.ehealthshield.backend.crypto.MlKemCryptoService;
import com.ehealthshield.backend.crypto.SseService;
import com.ehealthshield.backend.dto.request.EhrUploadRequest;
import com.ehealthshield.backend.dto.response.EhrSearchResultResponse;
import com.ehealthshield.backend.dto.response.EhrUploadResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.entity.UserEntity;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import com.ehealthshield.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class EhrService {

    public static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L; // 50 MB

    private final UserRepository userRepository;
    private final EhrRecordRepository ehrRecordRepository;
    private final HybridEncryptionService hybridEncryptionService;
    private final SseService sseService;
    private final BlockchainService blockchainService;

    public EhrService(UserRepository userRepository,
                      EhrRecordRepository ehrRecordRepository,
                      HybridEncryptionService hybridEncryptionService,
                      SseService sseService,
                      BlockchainService blockchainService) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository must not be null");
        this.ehrRecordRepository = Objects.requireNonNull(ehrRecordRepository, "EhrRecordRepository must not be null");
        this.hybridEncryptionService = Objects.requireNonNull(hybridEncryptionService, "HybridEncryptionService must not be null");
        this.sseService = Objects.requireNonNull(sseService, "SseService must not be null");
        this.blockchainService = Objects.requireNonNull(blockchainService, "BlockchainService must not be null");
    }

    /**
     * Uploads, validates, hybrid-encrypts, blindly indexes, and anchors an EHR document on Ethereum.
     * Persists only zero-knowledge ciphertexts and HMAC search tags.
     */
    @Transactional
    public EhrUploadResponse uploadEhr(EhrUploadRequest request) {
        Objects.requireNonNull(request, "EHR upload request must not be null");

        // 1. File Validation
        MultipartFile file = request.file();
        if (file == null || file.isEmpty()) {
            throw new ValidationException("EHR file must not be null or empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new ValidationException("EHR file must have a valid filename");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException("EHR file must have a valid content type");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("EHR file size exceeds maximum limit of 50 MB");
        }

        // 2. Wallet & Patient Validation
        String patientWallet = request.patientWallet();
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Patient wallet address must not be blank");
        }

        String uploaderWallet = request.uploaderWallet();
        if (uploaderWallet == null || uploaderWallet.isBlank()) {
            throw new ValidationException("Uploader wallet address must not be blank");
        }

        UserEntity patientUser = userRepository.findByWalletAddress(patientWallet)
                .orElseThrow(() -> new ResourceNotFoundException("Patient with wallet address " + patientWallet + " is not registered"));

        // 3. Retrieve Authoritative Patient ML-KEM-768 Public Key from Registered User
        byte[] patientPubKey = patientUser.getKyberPublicKey();
        if (patientPubKey == null || patientPubKey.length != MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES) {
            throw new ValidationException("Registered patient does not have a valid ML-KEM-768 public key (expected "
                    + MlKemCryptoService.PUBLIC_KEY_SIZE_BYTES + " bytes)");
        }

        // 4. Read File Bytes & Execute Hybrid Encryption
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ValidationException("Failed to read EHR file contents");
        }

        HybridEncryptionResult encryptionResult = hybridEncryptionService.encrypt(fileBytes, patientPubKey);

        // 5. Generate SSE HMAC-SHA256 Search Tags
        Set<String> searchTags = sseService.generateSearchTags(request.keywords());

        // 6. Pre-generate UUID for atomic blockchain registration
        UUID recordId = UUID.randomUUID();

        // 7. Blockchain Anchoring: Register record SHA-256 hash on Ethereum smart contract
        String txHash = blockchainService.registerRecord(
                recordId.toString(),
                patientWallet,
                uploaderWallet,
                encryptionResult.fileHash()
        );

        // 8. Build Zero-Knowledge EHR Record Entity with confirmed blockchain transaction hash
        EhrRecordEntity recordEntity = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet(patientWallet)
                .uploadedBy(uploaderWallet)
                .fileName(fileName)
                .contentType(contentType)
                .encryptedCiphertext(encryptionResult.encryptedCiphertext())
                .kemCiphertext(encryptionResult.kemCiphertext())
                .iv(encryptionResult.iv())
                .fileHash(encryptionResult.fileHash())
                .blockchainTransactionHash(txHash)
                .build();

        // 9. Associate Search Tags
        for (String tag : searchTags) {
            recordEntity.addSearchTag(tag);
        }

        // 10. Persist atomically
        EhrRecordEntity savedRecord = ehrRecordRepository.save(recordEntity);

        // 11. Build and return response
        return new EhrUploadResponse(
                savedRecord.getId(),
                savedRecord.getFileName(),
                savedRecord.getContentType(),
                savedRecord.getPatientWallet(),
                savedRecord.getFileHash(),
                savedRecord.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<EhrSearchResultResponse> getRecordsForPatient(String patientWallet) {
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Patient wallet address must not be blank");
        }

        return ehrRecordRepository.findByPatientWalletIgnoreCase(patientWallet.trim()).stream()
                .map(entity -> new EhrSearchResultResponse(
                        entity.getId(),
                        entity.getFileName(),
                        entity.getContentType(),
                        entity.getPatientWallet(),
                        entity.getUploadedBy(),
                        entity.getFileHash(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
