package com.ehealthshield.backend.service;

import com.ehealthshield.backend.blockchain.BlockchainService;
import com.ehealthshield.backend.crypto.SseService;
import com.ehealthshield.backend.dto.response.EhrSearchResultResponse;
import com.ehealthshield.backend.entity.EhrRecordEntity;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.repository.EhrRecordRepository;
import com.ehealthshield.backend.repository.RecordSearchTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SearchService {

    private final SseService sseService;
    private final RecordSearchTagRepository searchTagRepository;
    private final EhrRecordRepository ehrRecordRepository;
    private final BlockchainService blockchainService;

    public SearchService(SseService sseService,
                         RecordSearchTagRepository searchTagRepository,
                         EhrRecordRepository ehrRecordRepository,
                         BlockchainService blockchainService) {
        this.sseService = Objects.requireNonNull(sseService, "SseService must not be null");
        this.searchTagRepository = Objects.requireNonNull(searchTagRepository, "RecordSearchTagRepository must not be null");
        this.ehrRecordRepository = Objects.requireNonNull(ehrRecordRepository, "EhrRecordRepository must not be null");
        this.blockchainService = Objects.requireNonNull(blockchainService, "BlockchainService must not be null");
    }

    /**
     * Executes a blind keyword search over encrypted EHR records using SSE trapdoors
     * and logs the search event on the Ethereum audit trail.
     *
     * @param keyword the plaintext search keyword (normalized internally)
     * @param searcherWallet the accessor's wallet address (nullable for anonymous search)
     * @return list of matching record metadata (never plaintext content)
     */
    @Transactional(readOnly = true)
    public List<EhrSearchResultResponse> searchByKeyword(String keyword, String searcherWallet) {
        if (keyword == null || keyword.isBlank()) {
            throw new ValidationException("Search keyword must not be blank");
        }

        // 1. Generate HMAC-SHA256 trapdoor from the normalized keyword
        String trapdoor = sseService.generateTrapdoor(keyword);

        // 2. Query the search tag index for matching record IDs
        List<UUID> matchingRecordIds = searchTagRepository.findRecordIdsBySearchTag(trapdoor);

        // 3. Log search event on blockchain audit trail
        blockchainService.logSearchEvent(searcherWallet);

        if (matchingRecordIds.isEmpty()) {
            return List.of();
        }

        // 4. Fetch record entities and map to safe metadata responses
        List<EhrRecordEntity> records = ehrRecordRepository.findAllById(matchingRecordIds);

        return records.stream()
                .map(this::toSearchResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EhrSearchResultResponse> searchByKeyword(String keyword) {
        return searchByKeyword(keyword, null);
    }

    private EhrSearchResultResponse toSearchResult(EhrRecordEntity entity) {
        return new EhrSearchResultResponse(
                entity.getId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getPatientWallet(),
                entity.getUploadedBy(),
                entity.getFileHash(),
                entity.getCreatedAt()
        );
    }
}
