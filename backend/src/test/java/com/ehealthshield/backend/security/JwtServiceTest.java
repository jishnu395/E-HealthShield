package com.ehealthshield.backend.security;

import com.ehealthshield.backend.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_WALLET = "0x70997970c51812dc3a010c7d01b50e0d17dc79c8";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("my_test_super_secret_jwt_key_that_is_long_enough_256_bits_12345");
    }

    @Test
    @DisplayName("1. Token generation and claims extraction")
    void testTokenGenerationAndExtraction() {
        String token = jwtService.generateToken(TEST_WALLET, UserRole.PATIENT);
        assertNotNull(token);

        assertTrue(jwtService.validateToken(token));
        assertEquals(TEST_WALLET, jwtService.getWalletAddressFromToken(token));
        assertEquals(UserRole.PATIENT, jwtService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("2. Doctor role token generation and extraction")
    void testDoctorToken() {
        String doctorWallet = "0x90f79bf6eb2c4f870365e785982e1f101e93b906";
        String token = jwtService.generateToken(doctorWallet, UserRole.DOCTOR);

        assertTrue(jwtService.validateToken(token));
        assertEquals(doctorWallet, jwtService.getWalletAddressFromToken(token));
        assertEquals(UserRole.DOCTOR, jwtService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("3. Tampered token signature fails validation")
    void testTamperedTokenFails() {
        String token = jwtService.generateToken(TEST_WALLET, UserRole.PATIENT);
        String[] parts = token.split("\\.");

        // Tamper with payload
        String tamperedToken = parts[0] + "." + parts[1] + "x." + parts[2];
        assertFalse(jwtService.validateToken(tamperedToken));

        // Tamper with signature
        String tamperedSigToken = parts[0] + "." + parts[1] + ".tamperedsig";
        assertFalse(jwtService.validateToken(tamperedSigToken));
    }

    @Test
    @DisplayName("4. Malformed or null token fails validation gracefully")
    void testMalformedTokens() {
        assertFalse(jwtService.validateToken(null));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken("single-part-token"));
        assertFalse(jwtService.validateToken("part1.part2"));
    }
}
