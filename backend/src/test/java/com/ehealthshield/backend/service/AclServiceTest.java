package com.ehealthshield.backend.service;

import com.ehealthshield.backend.dto.response.AccessPermissionResponse;
import com.ehealthshield.backend.entity.AccessPermissionEntity;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.AccessPermissionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclServiceTest {

    @Mock
    private EhrRecordRepository ehrRecordRepository;

    @Mock
    private AccessPermissionRepository accessPermissionRepository;

    @Mock
    private com.ehealthshield.backend.blockchain.BlockchainService blockchainService;

    private AclService aclService;

    private static final String PATIENT_WALLET = "0x1111111111111111111111111111111111111111";
    private static final String UPLOADER_DOCTOR = "0x2222222222222222222222222222222222222222";
    private static final String OTHER_DOCTOR = "0x3333333333333333333333333333333333333333";

    private UUID recordId;
    private EhrRecordEntity record;

    @BeforeEach
    void setUp() {
        aclService = new AclService(ehrRecordRepository, accessPermissionRepository, blockchainService);
        recordId = UUID.randomUUID();

        record = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet(PATIENT_WALLET)
                .uploadedBy(UPLOADER_DOCTOR)
                .fileName("lab.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(new byte[]{1, 2, 3})
                .kemCiphertext(new byte[1088])
                .iv(new byte[12])
                .fileHash("00".repeat(32))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Patient owner always has access to their own record")
    void testPatientOwnerAlwaysHasAccess() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertTrue(aclService.checkAccess(recordId, PATIENT_WALLET));
        assertTrue(aclService.checkAccess(recordId, PATIENT_WALLET.toUpperCase()));
    }

    @Test
    @DisplayName("2. Uploader doctor always has access to the record they uploaded")
    void testUploaderDoctorAlwaysHasAccess() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertTrue(aclService.checkAccess(recordId, UPLOADER_DOCTOR));
    }

    @Test
    @DisplayName("3. Third-party doctor has access when explicitly granted in ACL")
    void testGrantedDoctorHasAccess() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(accessPermissionRepository.existsByRecordIdAndDoctorWalletAndIsGrantedTrue(recordId, OTHER_DOCTOR.toLowerCase()))
                .thenReturn(true);

        assertTrue(aclService.checkAccess(recordId, OTHER_DOCTOR));
    }

    @Test
    @DisplayName("4. Third-party doctor denied access when not granted in ACL")
    void testUnrelatedDoctorDeniedAccess() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(accessPermissionRepository.existsByRecordIdAndDoctorWalletAndIsGrantedTrue(recordId, OTHER_DOCTOR.toLowerCase()))
                .thenReturn(false);

        assertFalse(aclService.checkAccess(recordId, OTHER_DOCTOR));
    }

    @Test
    @DisplayName("5. Patient owner can grant access to a doctor")
    void testGrantAccessByPatientSucceeds() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(accessPermissionRepository.findByRecordIdAndDoctorWallet(recordId, OTHER_DOCTOR.toLowerCase()))
                .thenReturn(Optional.empty());
        when(accessPermissionRepository.save(any(AccessPermissionEntity.class)))
                .thenAnswer(inv -> {
                    AccessPermissionEntity entity = inv.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        AccessPermissionResponse response = aclService.grantAccess(recordId, OTHER_DOCTOR, PATIENT_WALLET);

        assertNotNull(response);
        assertEquals(recordId, response.recordId());
        assertEquals(OTHER_DOCTOR.toLowerCase(), response.doctorWallet());
        assertTrue(response.isGranted());
    }

    @Test
    @DisplayName("6. Non-owner cannot grant access to a record")
    void testGrantAccessByNonOwnerFails() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertThrows(ValidationException.class, () ->
                aclService.grantAccess(recordId, OTHER_DOCTOR, UPLOADER_DOCTOR));
    }

    @Test
    @DisplayName("7. Patient owner can revoke access from a doctor")
    void testRevokeAccessByPatientSucceeds() {
        AccessPermissionEntity existing = AccessPermissionEntity.builder()
                .id(1L)
                .recordId(recordId)
                .patientWallet(PATIENT_WALLET.toLowerCase())
                .doctorWallet(OTHER_DOCTOR.toLowerCase())
                .isGranted(true)
                .createdAt(Instant.now())
                .build();

        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(accessPermissionRepository.findByRecordIdAndDoctorWallet(recordId, OTHER_DOCTOR.toLowerCase()))
                .thenReturn(Optional.of(existing));
        when(accessPermissionRepository.save(any(AccessPermissionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccessPermissionResponse response = aclService.revokeAccess(recordId, OTHER_DOCTOR, PATIENT_WALLET);

        assertNotNull(response);
        assertFalse(response.isGranted());
    }

    @Test
    @DisplayName("8. Non-owner cannot revoke access from a record")
    void testRevokeAccessByNonOwnerFails() {
        when(ehrRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertThrows(ValidationException.class, () ->
                aclService.revokeAccess(recordId, OTHER_DOCTOR, "0x4444444444444444444444444444444444444444"));
    }
}
