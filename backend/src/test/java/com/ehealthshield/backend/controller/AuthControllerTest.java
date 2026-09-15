package com.ehealthshield.backend.controller;

import com.ehealthshield.backend.config.SecurityConfig;
import com.ehealthshield.backend.dto.request.AuthChallengeRequest;
import com.ehealthshield.backend.dto.request.AuthVerifyRequest;
import com.ehealthshield.backend.dto.request.UserRegisterRequest;
import com.ehealthshield.backend.dto.response.AuthChallengeResponse;
import com.ehealthshield.backend.dto.response.AuthResponse;
import com.ehealthshield.backend.entity.UserRole;
import com.ehealthshield.backend.exception.GlobalExceptionHandler;
import com.ehealthshield.backend.exception.ValidationException;
import com.ehealthshield.backend.security.JwtAuthenticationFilter;
import com.ehealthshield.backend.service.AuthService;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EhrService ehrService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private RetrievalService retrievalService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TEST_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";

    @Test
    @DisplayName("1. POST /api/auth/challenge - Successful challenge issuance")
    void testRequestChallengeSuccess() throws Exception {
        AuthChallengeResponse response = new AuthChallengeResponse(
                TEST_WALLET,
                "Sign this message to authenticate with E-HealthShield: Nonce 12345"
        );

        when(authService.requestChallenge(any(AuthChallengeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletAddress\": \"" + TEST_WALLET + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletAddress", is(TEST_WALLET)))
                .andExpect(jsonPath("$.challenge", notNullValue()));
    }

    @Test
    @DisplayName("2. POST /api/auth/verify - Successful signature verification and token issuance")
    void testVerifySignatureSuccess() throws Exception {
        AuthResponse response = new AuthResponse(
                "mock.jwt.token",
                TEST_WALLET,
                UserRole.PATIENT,
                86400L
        );

        when(authService.verifyAndAuthenticate(any(AuthVerifyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletAddress\": \"" + TEST_WALLET + "\", \"signature\": \"0x1234abcd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mock.jwt.token")))
                .andExpect(jsonPath("$.walletAddress", is(TEST_WALLET)))
                .andExpect(jsonPath("$.role", is("PATIENT")));
    }

    @Test
    @DisplayName("3. POST /api/auth/register - Successful registration")
    void testRegisterUserSuccess() throws Exception {
        AuthResponse response = new AuthResponse(
                "mock.jwt.token",
                TEST_WALLET,
                UserRole.DOCTOR,
                86400L
        );

        when(authService.registerUser(any(UserRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletAddress\": \"" + TEST_WALLET + "\", \"role\": \"DOCTOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("mock.jwt.token")))
                .andExpect(jsonPath("$.role", is("DOCTOR")));
    }

    @Test
    @DisplayName("4. POST /api/auth/verify - Invalid signature returns 400 Bad Request")
    void testVerifySignatureInvalidReturnsBadRequest() throws Exception {
        when(authService.verifyAndAuthenticate(any(AuthVerifyRequest.class)))
                .thenThrow(new ValidationException("Invalid Web3 cryptographic signature or expired challenge"));

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletAddress\": \"" + TEST_WALLET + "\", \"signature\": \"0xinvalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Error")));
    }
}
