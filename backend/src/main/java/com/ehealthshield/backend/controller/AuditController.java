package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.blockchain.OnChainRecordMetadata;
import com.ehealthshield.backend.dto.response.AuditVerificationResponse;
import com.ehealthshield.backend.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "AuditService must not be null");
    }

    /**
     * Conducts end-to-end cryptographic integrity audit against Ethereum Sepolia hash anchor.
     */
    @GetMapping("/verify/{recordId}")
    public ResponseEntity<AuditVerificationResponse> verifyIntegrity(@PathVariable("recordId") UUID recordId) {
        AuditVerificationResponse response = auditService.verifyRecordIntegrity(recordId);
        return ResponseEntity.ok(response);
    }

    /**
     * Queries on-chain smart contract metadata and block timestamp for an EHR record.
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<OnChainRecordMetadata> getOnChainRecord(@PathVariable("recordId") UUID recordId) {
        OnChainRecordMetadata response = auditService.getOnChainMetadata(recordId);
        return ResponseEntity.ok(response);
    }
}
