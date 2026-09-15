package com.ehealthshield.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_permissions", indexes = {
        @Index(name = "idx_access_permissions_record_id", columnList = "record_id"),
        @Index(name = "idx_access_permissions_patient_wallet", columnList = "patient_wallet"),
        @Index(name = "idx_access_permissions_doctor_wallet", columnList = "doctor_wallet"),
        @Index(name = "idx_access_permissions_lookup", columnList = "record_id, doctor_wallet")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "patient_wallet", nullable = false, length = 42)
    private String patientWallet;

    @Column(name = "doctor_wallet", nullable = false, length = 42)
    private String doctorWallet;

    @Column(name = "is_granted", nullable = false)
    private boolean isGranted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
