package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.dto.request.EhrUploadRequest;
import com.ehealthshield.backend.dto.response.EhrUploadResponse;
import com.ehealthshield.backend.exception.CryptographicException;
import com.ehealthshield.backend.exception.GlobalExceptionHandler;
import com.ehealthshield.backend.exception.ResourceNotFoundException;
import com.ehealthshield.backend.service.EhrService;
import com.ehealthshield.backend.service.SearchService;
import com.ehealthshield.backend.service.RetrievalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ehealthshield.backend.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EhrController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class EhrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EhrService ehrService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private RetrievalService retrievalService;

    @MockitoBean
    private com.ehealthshield.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String PATIENT_WALLET = "0x1234567890123456789012345678901234567890";
    private static final String DOCTOR_WALLET = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd";

    @Test
    @DisplayName("1. Successful upload returns 201 Created and expected JSON response")
    void testSuccessfulUploadRequest() throws Exception {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.now();
        String fileHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        EhrUploadResponse mockResponse = new EhrUploadResponse(
                recordId,
                "blood_test.pdf",
                "application/pdf",
                PATIENT_WALLET,
                fileHash,
                now
        );

        when(ehrService.uploadEhr(any(EhrUploadRequest.class))).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blood_test.pdf",
                "application/pdf",
                "Patient Blood Test Data".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "blood", "cholesterol", "lab")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.fileName", is("blood_test.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.patientWallet", is(PATIENT_WALLET)))
                .andExpect(jsonPath("$.fileHash", is(fileHash)))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("2. Missing file parameter returns 400 Bad Request")
    void testMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/ehrs/upload")
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "glucose")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("3. Empty file returns 400 Bad Request")
    void testEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(emptyFile)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "cardio")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("EHR file is required and must not be empty")));
    }

    @Test
    @DisplayName("4. Missing patient wallet returns 400 Bad Request")
    void testMissingPatientWallet() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "Scan Content".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", "")
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "mri")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("patientWallet is required")));
    }

    @Test
    @DisplayName("5. Invalid wallet address format returns 400 Bad Request")
    void testInvalidWalletAddress() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "Scan Content".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", "invalid-wallet-address")
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "mri")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid patientWallet format: must be a valid 42-character hexadecimal Ethereum address (e.g. 0x...)")));
    }

    @Test
    @DisplayName("6. Missing uploader wallet returns 400 Bad Request")
    void testMissingUploaderWallet() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "Scan Content".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", "")
                        .param("keywords", "mri")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("uploaderWallet is required")));
    }

    @Test
    @DisplayName("7. Missing keywords returns 400 Bad Request")
    void testMissingKeywords() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "Scan Content".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("keywords are required and must not be empty")));
    }

    @Test
    @DisplayName("8. ResourceNotFoundException and CryptographicException handled with proper HTTP status")
    void testServiceExceptionHandling() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "Scan Content".getBytes()
        );

        // Case A: Patient Not Found -> 404
        when(ehrService.uploadEhr(any(EhrUploadRequest.class)))
                .thenThrow(new ResourceNotFoundException("Patient with wallet address " + PATIENT_WALLET + " is not registered"));

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "oncology")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Resource Not Found")))
                .andExpect(jsonPath("$.message", is("Patient with wallet address " + PATIENT_WALLET + " is not registered")));

        // Case B: Cryptographic Failure -> 500
        when(ehrService.uploadEhr(any(EhrUploadRequest.class)))
                .thenThrow(new CryptographicException("ML-KEM encapsulation failed"));

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "oncology")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Cryptographic Error")))
                .andExpect(jsonPath("$.message", is("ML-KEM encapsulation failed")));
    }

    @Test
    @DisplayName("9. Response structure strictly omits plaintext EHR content, keys, or search keywords")
    void testSuccessfulResponseStructureZeroKnowledge() throws Exception {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.now();
        String fileHash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";

        EhrUploadResponse mockResponse = new EhrUploadResponse(
                recordId,
                "prescription.pdf",
                "application/pdf",
                PATIENT_WALLET,
                fileHash,
                now
        );

        when(ehrService.uploadEhr(any(EhrUploadRequest.class))).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "prescription.pdf",
                "application/pdf",
                "Confidential Plaintext Rx Data".getBytes()
        );

        mockMvc.perform(multipart("/api/ehrs/upload")
                        .file(file)
                        .param("patientWallet", PATIENT_WALLET)
                        .param("uploaderWallet", DOCTOR_WALLET)
                        .param("keywords", "amoxicillin", "dosage")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId", is(recordId.toString())))
                .andExpect(jsonPath("$.fileName", is("prescription.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.patientWallet", is(PATIENT_WALLET)))
                .andExpect(jsonPath("$.fileHash", is(fileHash)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.key").doesNotExist())
                .andExpect(jsonPath("$.aesKey").doesNotExist())
                .andExpect(jsonPath("$.sharedSecret").doesNotExist())
                .andExpect(jsonPath("$.privateKey").doesNotExist())
                .andExpect(jsonPath("$.keywords").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist());
    }
}
