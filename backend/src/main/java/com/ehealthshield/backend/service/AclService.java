package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.dto.response.AccessPermissionResponse;
import com.ehealthshield.backend.entity.AccessPermissionEntity;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.AccessPermissionRepository;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AclService {

    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final EhrRecordRepository ehrRecordRepository;
    private final AccessPermissionRepository accessPermissionRepository;
    private final BlockchainService blockchainService;

    public AclService(EhrRecordRepository ehrRecordRepository,
                      AccessPermissionRepository accessPermissionRepository,
                      BlockchainService blockchainService) {
        this.ehrRecordRepository = Objects.requireNonNull(ehrRecordRepository, "EhrRecordRepository must not be null");
        this.accessPermissionRepository = Objects.requireNonNull(accessPermissionRepository, "AccessPermissionRepository must not be null");
        this.blockchainService = Objects.requireNonNull(blockchainService, "BlockchainService must not be null");
    }

    /**
     * Evaluates whether an accessor wallet has permission to retrieve an EHR record.
     * Rules from PROJECT_SPEC.md:
     * 1. Patient Owner: Always authorized.
     * 2. Uploader Doctor: Always authorized.
     * 3. Third-party Doctor: Authorized if active on-chain/ACL grant exists (is_granted = true).
     */
    @Transactional(readOnly = true)
    public boolean checkAccess(UUID recordId, String accessorWallet) {
        if (recordId == null || accessorWallet == null || accessorWallet.isBlank()) {
            return false;
        }

        String normalizedAccessor = accessorWallet.trim().toLowerCase();

        EhrRecordEntity record = ehrRecordRepository.findById(recordId).orElse(null);
        if (record == null) {
            return false;
        }

        // Rule 1: Record Owner
        if (record.getPatientWallet().equalsIgnoreCase(normalizedAccessor)) {
            return true;
        }

        // Rule 2: Original Uploader
        if (record.getUploadedBy().equalsIgnoreCase(normalizedAccessor)) {
            return true;
        }

        // Rule 3: Explicit ACL Permission Grant (verified across DB and smart contract state)
        boolean dbGranted = accessPermissionRepository.existsByRecordIdAndDoctorWalletAndIsGrantedTrue(recordId, normalizedAccessor);
        boolean chainGranted = blockchainService.checkAccess(recordId.toString(), normalizedAccessor);

        return dbGranted || chainGranted;
    }

    /**
     * Grants a doctor access to an EHR record on both database and blockchain ACLs.
     */
    @Transactional
    public AccessPermissionResponse grantAccess(UUID recordId, String doctorWallet, String patientWallet) {
        validateInputs(recordId, doctorWallet, patientWallet);

        EhrRecordEntity record = ehrRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("EHR record with ID " + recordId + " not found"));

        if (!record.getPatientWallet().equalsIgnoreCase(patientWallet.trim())) {
            throw new ValidationException("Access denied: only the record patient owner can grant permissions");
        }

        String normalizedDoctor = doctorWallet.trim().toLowerCase();
        String normalizedPatient = patientWallet.trim().toLowerCase();

        // 1. Sync grant to Ethereum Smart Contract ACL
        String txHash = blockchainService.grantAccess(recordId.toString(), normalizedPatient, normalizedDoctor);

        // 2. Persist in database
        AccessPermissionEntity permission = accessPermissionRepository
                .findByRecordIdAndDoctorWallet(recordId, normalizedDoctor)
                .orElseGet(() -> AccessPermissionEntity.builder()
                        .recordId(recordId)
                        .patientWallet(normalizedPatient)
                        .doctorWallet(normalizedDoctor)
                        .createdAt(Instant.now())
                        .build());

        permission.setGranted(true);
        permission.setUpdatedAt(Instant.now());

        AccessPermissionEntity saved = accessPermissionRepository.save(permission);
        return toResponse(saved, txHash);
    }

    /**
     * Revokes a doctor's access to an EHR record on both database and blockchain ACLs.
     */
    @Transactional
    public AccessPermissionResponse revokeAccess(UUID recordId, String doctorWallet, String patientWallet) {
        validateInputs(recordId, doctorWallet, patientWallet);

        EhrRecordEntity record = ehrRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("EHR record with ID " + recordId + " not found"));

        if (!record.getPatientWallet().equalsIgnoreCase(patientWallet.trim())) {
            throw new ValidationException("Access denied: only the record patient owner can revoke permissions");
        }

        String normalizedDoctor = doctorWallet.trim().toLowerCase();
        String normalizedPatient = patientWallet.trim().toLowerCase();

        // 1. Sync revocation to Ethereum Smart Contract ACL
        String txHash = blockchainService.revokeAccess(recordId.toString(), normalizedPatient, normalizedDoctor);

        // 2. Update database
        AccessPermissionEntity permission = accessPermissionRepository
                .findByRecordIdAndDoctorWallet(recordId, normalizedDoctor)
                .orElseGet(() -> AccessPermissionEntity.builder()
                        .recordId(recordId)
                        .patientWallet(normalizedPatient)
                        .doctorWallet(normalizedDoctor)
                        .createdAt(Instant.now())
                        .build());

        permission.setGranted(false);
        permission.setUpdatedAt(Instant.now());

        AccessPermissionEntity saved = accessPermissionRepository.save(permission);
        return toResponse(saved, txHash);
    }

    /**
     * Lists all permission grants for a record. Only the patient owner can view this.
     */
    @Transactional(readOnly = true)
    public List<AccessPermissionResponse> getPermissionsForRecord(UUID recordId, String requesterWallet) {
        if (recordId == null || requesterWallet == null) {
            throw new ValidationException("Record ID and requester wallet are required");
        }

        EhrRecordEntity record = ehrRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("EHR record with ID " + recordId + " not found"));

        if (!record.getPatientWallet().equalsIgnoreCase(requesterWallet.trim())) {
            throw new ValidationException("Access denied: only the patient owner can view permissions for this record");
        }

        return accessPermissionRepository.findByRecordId(recordId).stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccessPermissionResponse> getPermissionsForPatient(String patientWallet) {
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("Patient wallet address is required");
        }

        return accessPermissionRepository.findByPatientWallet(patientWallet.trim().toLowerCase()).stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    private void validateInputs(UUID recordId, String doctorWallet, String patientWallet) {
        if (recordId == null) {
            throw new ValidationException("Record ID is required");
        }
        if (doctorWallet == null || !ETHEREUM_ADDRESS_PATTERN.matcher(doctorWallet.trim()).matches()) {
            throw new ValidationException("Invalid doctor wallet address format");
        }
        if (patientWallet == null || !ETHEREUM_ADDRESS_PATTERN.matcher(patientWallet.trim()).matches()) {
            throw new ValidationException("Invalid patient wallet address format");
        }
    }

    private AccessPermissionResponse toResponse(AccessPermissionEntity entity, String txHash) {
        return new AccessPermissionResponse(
                entity.getId(),
                entity.getRecordId(),
                entity.getPatientWallet(),
                entity.getDoctorWallet(),
                entity.isGranted(),
                txHash,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
