package com.ehealthshield.backend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EhrRecordEntityLifecycleTest {

    @Test
    @DisplayName("1. Fresh entity with pre-assigned UUID reports isNew = true for persist")
    void testFreshEntityWithPreAssignedIdReportsIsNewTrue() {
        UUID recordId = UUID.randomUUID();

        EhrRecordEntity entity = EhrRecordEntity.builder()
                .id(recordId)
                .patientWallet("0x70997970C51812dc3A010C7d01b50e0d17dc79C8")
                .uploadedBy("0x90F79bf6EB2c4f870365E785982E1f101E93b906")
                .fileName("test.pdf")
                .contentType("application/pdf")
                .encryptedCiphertext(new byte[]{1, 2, 3})
                .kemCiphertext(new byte[1088])
                .iv(new byte[12])
                .fileHash("00".repeat(32))
                .build();

        assertEquals(recordId, entity.getId());
        assertTrue(entity.isNew(), "Freshly constructed entity must report isNew() == true so JPA invokes persist() instead of merge()");
    }

    @Test
    @DisplayName("2. PrePersist generates ID if null and populates createdAt")
    void testPrePersistGeneratesIdAndCreatedAt() {
        EhrRecordEntity entity = EhrRecordEntity.builder()
                .patientWallet("0x70997970C51812dc3A010C7d01b50e0d17dc79C8")
                .uploadedBy("0x90F79bf6EB2c4f870365E785982E1f101E93b906")
                .fileName("test.pdf")
                .contentType("application/pdf")
                .build();

        entity.onCreate();

        assertNotNull(entity.getId(), "PrePersist must assign UUID if none was provided");
        assertNotNull(entity.getCreatedAt(), "PrePersist must populate createdAt timestamp");
    }

    @Test
    @DisplayName("3. PostPersist / PostLoad transitions entity state to isNew = false")
    void testPostPersistMarksNotNew() {
        UUID recordId = UUID.randomUUID();

        EhrRecordEntity entity = EhrRecordEntity.builder()
                .id(recordId)
                .createdAt(Instant.now())
                .build();

        entity.markNotNew();

        assertFalse(entity.isNew(), "Persisted or loaded entity must report isNew() == false");
    }
}
