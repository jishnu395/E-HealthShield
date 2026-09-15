package com.ehealthshield.backend.repository;

import com.ehealthshield.backend.entity.AccessPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessPermissionRepository extends JpaRepository<AccessPermissionEntity, Long> {

    Optional<AccessPermissionEntity> findByRecordIdAndDoctorWallet(UUID recordId, String doctorWallet);

    boolean existsByRecordIdAndDoctorWalletAndIsGrantedTrue(UUID recordId, String doctorWallet);

    List<AccessPermissionEntity> findByRecordId(UUID recordId);

    List<AccessPermissionEntity> findByPatientWallet(String patientWallet);

    List<AccessPermissionEntity> findByDoctorWallet(String doctorWallet);
}
