package com.ehealthshield.backend.repository;

import com.ehealthshield.backend.entity.RecordSearchTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecordSearchTagRepository extends JpaRepository<RecordSearchTagEntity, Long> {
    List<RecordSearchTagEntity> findBySearchTag(String searchTag);

    @Query("SELECT DISTINCT t.ehrRecord.id FROM RecordSearchTagEntity t WHERE t.searchTag = :searchTag")
    List<UUID> findRecordIdsBySearchTag(@Param("searchTag") String searchTag);

    List<RecordSearchTagEntity> findByEhrRecord_Id(UUID recordId);
}
