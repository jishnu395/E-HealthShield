package com.ehealthshield.backend.repository;

import com.ehealthshield.backend.entity.EhrRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EhrRecordRepository extends JpaRepository<EhrRecordEntity, UUID> {
    List<EhrRecordEntity> findByPatientWallet(String patientWallet);
    List<EhrRecordEntity> findByPatientWalletIgnoreCase(String patientWallet);
    List<EhrRecordEntity> findByUploadedBy(String uploadedBy);
    Optional<EhrRecordEntity> findByFileHash(String fileHash);
}
