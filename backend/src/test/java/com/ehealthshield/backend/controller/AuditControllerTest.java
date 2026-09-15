package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.blockchain.OnChainRecordMetadata;
import com.ehealthshield.backend.config.SecurityConfig;
import com.ehealthshield.backend.dto.response.AuditVerificationResponse;
import com.ehealthshield.backend.exception.GlobalExceptionHandler;
import com.ehealthshield.backend.security.JwtAuthenticationFilter;
import com.ehealthshield.backend.service.AuditService;
import com.ehealthshield.backend.service.EhrService;
import com.ehealthshield.backend.service.RetrievalService;
import com.ehealthshield.backend.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private EhrService ehrService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private RetrievalService retrievalService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("1. GET /api/audit/verify/{id} - Returns verified tamper-free audit response")
    void testVerifyIntegrityEndpoint() throws Exception {
        UUID recordId = UUID.randomUUID();
        String fileHash = "aa".repeat(32);
        String txHash = "0x" + "bb".repeat(32);

        AuditVerificationResponse response = new AuditVerificationResponse(
                recordId, fileHash, fileHash, txHash, true, 1700000000L
        );

        when(auditService.verifyRecordIntegrity(recordId)).thenReturn(response);

        mockMvc.perform(get("/api/audit/verify/" + recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.isTamperFree", is(true)))
                .andExpect(jsonPath("$.localCiphertextHash", is(fileHash)))
                .andExpect(jsonPath("$.onChainAnchoredHash", is(fileHash)));
    }

    @Test
    @DisplayName("2. GET /api/audit/record/{id} - Returns on-chain record metadata")
    void testGetOnChainRecordEndpoint() throws Exception {
        UUID recordId = UUID.randomUUID();
        OnChainRecordMetadata metadata = new OnChainRecordMetadata(
                "aa".repeat(32),
                "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
                "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
                1700000000L,
                true
        );

        when(auditService.getOnChainMetadata(recordId)).thenReturn(metadata);

        mockMvc.perform(get("/api/audit/record/" + recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)))
                .andExpect(jsonPath("$.ownerPatient", is("0x70997970C51812dc3A010C7d01b50e0d17dc79C8")));
    }
}
