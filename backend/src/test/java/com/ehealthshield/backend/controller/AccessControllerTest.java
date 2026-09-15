package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.config.SecurityConfig;
import com.ehealthshield.backend.dto.response.AccessPermissionResponse;
import com.ehealthshield.backend.exception.GlobalExceptionHandler;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.security.JwtAuthenticationFilter;
import com.ehealthshield.backend.service.AclService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AclService aclService;

    @MockitoBean
    private EhrService ehrService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private RetrievalService retrievalService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String PATIENT_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
    private static final String DOCTOR_WALLET = "0x90F79bf6EB2c4f870365E785982E1f101E93b906";

    @Test
    @DisplayName("1. POST /api/access/grant - Successfully grant access")
    void testGrantAccessSuccess() throws Exception {
        UUID recordId = UUID.randomUUID();
        AccessPermissionResponse response = new AccessPermissionResponse(
                1L, recordId, PATIENT_WALLET, DOCTOR_WALLET, true, Instant.now(), Instant.now()
        );

        when(aclService.grantAccess(eq(recordId), eq(DOCTOR_WALLET), any())).thenReturn(response);

        mockMvc.perform(post("/api/access/grant")
                        .param("patientWallet", PATIENT_WALLET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\": \"" + recordId + "\", \"doctorWallet\": \"" + DOCTOR_WALLET + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.doctorWallet", is(DOCTOR_WALLET)))
                .andExpect(jsonPath("$.isGranted", is(true)));
    }

    @Test
    @DisplayName("2. POST /api/access/revoke - Successfully revoke access")
    void testRevokeAccessSuccess() throws Exception {
        UUID recordId = UUID.randomUUID();
        AccessPermissionResponse response = new AccessPermissionResponse(
                1L, recordId, PATIENT_WALLET, DOCTOR_WALLET, false, Instant.now(), Instant.now()
        );

        when(aclService.revokeAccess(eq(recordId), eq(DOCTOR_WALLET), any())).thenReturn(response);

        mockMvc.perform(post("/api/access/revoke")
                        .param("patientWallet", PATIENT_WALLET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\": \"" + recordId + "\", \"doctorWallet\": \"" + DOCTOR_WALLET + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.isGranted", is(false)));
    }

    @Test
    @DisplayName("3. GET /api/access/check - Check access returns boolean flag")
    void testCheckAccessSuccess() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(aclService.checkAccess(recordId, DOCTOR_WALLET)).thenReturn(true);

        mockMvc.perform(get("/api/access/check")
                        .param("recordId", recordId.toString())
                        .param("doctorWallet", DOCTOR_WALLET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.doctorWallet", is(DOCTOR_WALLET)))
                .andExpect(jsonPath("$.hasAccess", is(true)));
    }

    @Test
    @DisplayName("4. POST /api/access/grant - Non-owner grant returns 400 Bad Request")
    void testGrantAccessUnauthorizedReturnsBadRequest() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(aclService.grantAccess(any(), any(), any()))
                .thenThrow(new ValidationException("Access denied: only the record patient owner can grant permissions"));

        mockMvc.perform(post("/api/access/grant")
                        .param("patientWallet", "0x0000000000000000000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\": \"" + recordId + "\", \"doctorWallet\": \"" + DOCTOR_WALLET + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Access denied: only the record patient owner can grant permissions")));
    }
}
