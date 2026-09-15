package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.dto.request.EhrSearchRequest;
import com.ehealthshield.backend.dto.request.EhrUploadRequest;
import com.ehealthshield.backend.dto.response.EhrRetrievalResponse;
import com.ehealthshield.backend.dto.response.EhrSearchResultResponse;
import com.ehealthshield.backend.dto.response.EhrUploadResponse;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.service.EhrService;
import com.ehealthshield.backend.service.RetrievalService;
import com.ehealthshield.backend.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ehrs")
public class EhrController {

    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    public static final int MAX_KEYWORD_COUNT = 50;

    private final EhrService ehrService;
    private final SearchService searchService;
    private final RetrievalService retrievalService;

    public EhrController(EhrService ehrService,
                         SearchService searchService,
                         RetrievalService retrievalService) {
        this.ehrService = Objects.requireNonNull(ehrService, "EhrService must not be null");
        this.searchService = Objects.requireNonNull(searchService, "SearchService must not be null");
        this.retrievalService = Objects.requireNonNull(retrievalService, "RetrievalService must not be null");
    }

    // ==================== UPLOAD ====================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EhrUploadResponse> uploadEhr(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientWallet") String patientWallet,
            @RequestParam("uploaderWallet") String uploaderWallet,
            @RequestParam(value = "keywords", required = false) List<String> keywords) {

        // 1. File Validation
        if (file == null || file.isEmpty()) {
            throw new ValidationException("EHR file is required and must not be empty");
        }

        // 2. Patient Wallet Validation
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("patientWallet is required");
        }
        if (!ETHEREUM_ADDRESS_PATTERN.matcher(patientWallet.trim()).matches()) {
            throw new ValidationException("Invalid patientWallet format: must be a valid 42-character hexadecimal Ethereum address (e.g. 0x...)");
        }

        // 3. Uploader Wallet Validation
        if (uploaderWallet == null || uploaderWallet.isBlank()) {
            throw new ValidationException("uploaderWallet is required");
        }
        if (!ETHEREUM_ADDRESS_PATTERN.matcher(uploaderWallet.trim()).matches()) {
            throw new ValidationException("Invalid uploaderWallet format: must be a valid 42-character hexadecimal Ethereum address (e.g. 0x...)");
        }

        // 4. Keywords Validation
        if (keywords == null || keywords.isEmpty()) {
            throw new ValidationException("keywords are required and must not be empty");
        }
        List<String> cleanedKeywords = keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .toList();

        if (cleanedKeywords.isEmpty()) {
            throw new ValidationException("At least one non-empty keyword is required for blind SSE indexing");
        }
        if (cleanedKeywords.size() > MAX_KEYWORD_COUNT) {
            throw new ValidationException("Keyword count exceeds maximum allowed limit of " + MAX_KEYWORD_COUNT);
        }

        // 5. Delegate to Business Service
        EhrUploadRequest request = new EhrUploadRequest(file, patientWallet.trim(), uploaderWallet.trim(), cleanedKeywords);
        EhrUploadResponse response = ehrService.uploadEhr(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== SEARCH ====================

    /**
     * Blind keyword search over encrypted EHR records using SSE trapdoors.
     * The keyword is converted to an HMAC-SHA256 trapdoor server-side.
     * Returns safe metadata only — never plaintext EHR content or keys.
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EhrSearchResultResponse>> searchEhr(@RequestBody EhrSearchRequest request) {
        if (request == null || request.keyword() == null || request.keyword().isBlank()) {
            throw new ValidationException("Search keyword must not be blank");
        }

        List<EhrSearchResultResponse> results = searchService.searchByKeyword(request.keyword());

        return ResponseEntity.ok(results);
    }

    // ==================== RETRIEVAL ====================

    /**
     * Retrieves an encrypted EHR bundle by record UUID for client-side decryption.
     * Enforces smart contract / ACL authorization against the requester identity.
     * The backend NEVER decrypts the content — the client must use their ML-KEM-768
     * private key to decapsulate the AES key and decrypt locally.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<EhrRetrievalResponse> downloadEhr(
            @PathVariable("id") String id,
            @RequestParam(value = "requesterWallet", required = false) String requesterWalletParam,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.ehealthshield.backend.security.UserPrincipal principal) {

        UUID recordId;
        try {
            recordId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid record ID format: must be a valid UUID");
        }

        String requesterWallet = principal != null ? principal.getWalletAddress() : requesterWalletParam;
        EhrRetrievalResponse response = retrievalService.retrieveEhr(recordId, requesterWallet);

        return ResponseEntity.ok(response);
    }

    /**
     * Lists all registered EHR records for a patient.
     */
    @GetMapping("/patient/{patientWallet}")
    public ResponseEntity<List<EhrSearchResultResponse>> getRecordsByPatient(
            @PathVariable("patientWallet") String patientWallet) {
        if (patientWallet == null || patientWallet.isBlank()) {
            throw new ValidationException("patientWallet is required");
        }
        List<EhrSearchResultResponse> records = ehrService.getRecordsForPatient(patientWallet);
        return ResponseEntity.ok(records);
    }
}
