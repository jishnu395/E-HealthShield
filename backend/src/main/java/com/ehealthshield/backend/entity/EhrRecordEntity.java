package com.ehealthshield.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ehr_records", indexes = {
        @Index(name = "idx_ehr_records_patient_wallet", columnList = "patient_wallet"),
        @Index(name = "idx_ehr_records_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_ehr_records_file_hash", columnList = "file_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EhrRecordEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_wallet", nullable = false, length = 42)
    private String patientWallet;

    @Column(name = "uploaded_by", nullable = false, length = 42)
    private String uploadedBy;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "encrypted_ciphertext", columnDefinition = "BYTEA", nullable = false)
    private byte[] encryptedCiphertext;

    @Column(name = "kem_ciphertext", columnDefinition = "BYTEA", nullable = false)
    private byte[] kemCiphertext;

    @Column(name = "iv", columnDefinition = "BYTEA", nullable = false)
    private byte[] iv;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTransactionHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "ehrRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordSearchTagEntity> searchTags = new ArrayList<>();

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew || this.createdAt == null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        this.isNew = false;
    }

    public void addSearchTag(String searchTag) {
        RecordSearchTagEntity tagEntity = RecordSearchTagEntity.builder()
                .ehrRecord(this)
                .searchTag(searchTag)
                .build();
        this.searchTags.add(tagEntity);
    }
}
