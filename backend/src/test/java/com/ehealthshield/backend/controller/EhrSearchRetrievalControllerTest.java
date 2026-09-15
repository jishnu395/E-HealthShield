package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.config.SecurityConfig;
import com.ehealthshield.backend.dto.response.EhrRetrievalResponse;
import com.ehealthshield.backend.dto.response.EhrSearchResultResponse;
import com.ehealthshield.backend.exception.GlobalExceptionHandler;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.security.JwtAuthenticationFilter;
import com.ehealthshield.backend.service.EhrService;
import com.ehealthshield.backend.service.RetrievalService;
import com.ehealthshield.backend.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EhrController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class EhrSearchRetrievalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EhrService ehrService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private RetrievalService retrievalService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String PATIENT_WALLET = "0x1234567890123456789012345678901234567890";
    private static final String DOCTOR_WALLET = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd";

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("S1. Successful keyword search returns 200 OK with matching results")
    void testSuccessfulSearch() throws Exception {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.now();
        String fileHash = "aabb" + "cc".repeat(30);

        EhrSearchResultResponse result = new EhrSearchResultResponse(
                recordId, "blood_test.pdf", "application/pdf",
                PATIENT_WALLET, DOCTOR_WALLET, fileHash, now
        );

        when(searchService.searchByKeyword("cardiology")).thenReturn(List.of(result));

        mockMvc.perform(post("/api/ehrs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"cardiology\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recordId", is(recordId.toString())))
                .andExpect(jsonPath("$[0].fileName", is("blood_test.pdf")))
                .andExpect(jsonPath("$[0].contentType", is("application/pdf")))
                .andExpect(jsonPath("$[0].patientWallet", is(PATIENT_WALLET)))
                .andExpect(jsonPath("$[0].uploaderWallet", is(DOCTOR_WALLET)))
                .andExpect(jsonPath("$[0].fileHash", is(fileHash)))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));
    }

    @Test
    @DisplayName("S2. Non-matching keyword returns 200 OK with empty list")
    void testNonMatchingKeywordReturnsEmptyList() throws Exception {
        when(searchService.searchByKeyword("nonexistent")).thenReturn(List.of());

        mockMvc.perform(post("/api/ehrs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"nonexistent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("S3. Blank keyword returns 400 Bad Request")
    void testBlankKeywordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/ehrs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("S4. Null keyword returns 400 Bad Request")
    void testNullKeywordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/ehrs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("S5. Search response never contains plaintext EHR content or keys")
    void testSearchResponseZeroKnowledge() throws Exception {
        UUID recordId = UUID.randomUUID();
        EhrSearchResultResponse result = new EhrSearchResultResponse(
                recordId, "lab.pdf", "application/pdf",
                PATIENT_WALLET, DOCTOR_WALLET, "ff".repeat(32), Instant.now()
        );

        when(searchService.searchByKeyword("glucose")).thenReturn(List.of(result));

        mockMvc.perform(post("/api/ehrs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\": \"glucose\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId", notNullValue()))
                .andExpect(jsonPath("$[0].encryptedCiphertext").doesNotExist())
                .andExpect(jsonPath("$[0].kemCiphertext").doesNotExist())
                .andExpect(jsonPath("$[0].iv").doesNotExist())
                .andExpect(jsonPath("$[0].aesKey").doesNotExist())
                .andExpect(jsonPath("$[0].sharedSecret").doesNotExist())
                .andExpect(jsonPath("$[0].privateKey").doesNotExist())
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    // ==================== RETRIEVAL TESTS ====================

    @Test
    @DisplayName("R1. Successful retrieval returns 200 OK with encrypted bundle")
    void testSuccessfulRetrieval() throws Exception {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.now();

        EhrRetrievalResponse response = new EhrRetrievalResponse(
                recordId, "ecg.pdf", "application/pdf",
                PATIENT_WALLET, DOCTOR_WALLET,
                new byte[]{1, 2, 3, 4, 5},  // encryptedCiphertext
                new byte[12],               // iv
                new byte[1088],             // kemCiphertext
                "dd".repeat(32),            // fileHash
                now
        );

        when(retrievalService.retrieveEhr(eq(recordId), any())).thenReturn(response);

        mockMvc.perform(get("/api/ehrs/" + recordId + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.fileName", is("ecg.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.patientWallet", is(PATIENT_WALLET)))
                .andExpect(jsonPath("$.uploaderWallet", is(DOCTOR_WALLET)))
                .andExpect(jsonPath("$.encryptedCiphertext", notNullValue()))
                .andExpect(jsonPath("$.iv", notNullValue()))
                .andExpect(jsonPath("$.kemCiphertext", notNullValue()))
                .andExpect(jsonPath("$.fileHash", is("dd".repeat(32))))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("R2. Nonexistent record ID returns 404 Not Found")
    void testNonexistentRecordReturnsNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(retrievalService.retrieveEhr(eq(missingId), any()))
                .thenThrow(new ResourceNotFoundException("EHR record with ID " + missingId + " not found"));

        mockMvc.perform(get("/api/ehrs/" + missingId + "/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Resource Not Found")));
    }

    @Test
    @DisplayName("R3. Invalid UUID format returns 400 Bad Request")
    void testInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/ehrs/not-a-valid-uuid/download"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid record ID format: must be a valid UUID")));
    }
}
