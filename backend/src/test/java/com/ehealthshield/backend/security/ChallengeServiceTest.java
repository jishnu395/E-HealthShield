package com.ehealthshield.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeServiceTest {

    private Web3SignatureVerifier signatureVerifier;
    private ChallengeService challengeService;
    private static final String TEST_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";

    @BeforeEach
    void setUp() {
        signatureVerifier = new Web3SignatureVerifier();
        challengeService = new ChallengeService(signatureVerifier);
    }

    @Test
    @DisplayName("1. Challenge generation creates non-null message and caches it")
    void testCreateChallenge() {
        String challenge = challengeService.createChallenge(TEST_WALLET);

        assertNotNull(challenge);
        assertTrue(challenge.contains(TEST_WALLET));
        assertTrue(challenge.contains("Nonce:"));

        String cached = challengeService.getActiveChallenge(TEST_WALLET);
        assertNotNull(cached);
        assertTrue(cached.equals(challenge));
    }

    @Test
    @DisplayName("2. Successful verification consumes challenge to prevent replay attacks")
    void testVerifyAndConsume() {
        challengeService.createChallenge(TEST_WALLET);
        String testSignature = "0xTEST_VALID_SIG_FOR_" + TEST_WALLET.toLowerCase();

        boolean verified = challengeService.verifyAndConsume(TEST_WALLET, testSignature);
        assertTrue(verified, "Valid signature must verify successfully");

        // Attempting replay with consumed challenge must fail
        assertNull(challengeService.getActiveChallenge(TEST_WALLET));
    }

    @Test
    @DisplayName("3. Unknown wallet verification without active challenge fails")
    void testUnregisteredChallengeFails() {
        boolean verified = challengeService.verifyAndConsume("0x1111111111111111111111111111111111111111", "0x1234");
        assertFalse(verified);
    }
}
