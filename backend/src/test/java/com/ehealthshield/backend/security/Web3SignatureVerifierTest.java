package com.ehealthshield.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Web3SignatureVerifierTest {

    private Web3SignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new Web3SignatureVerifier();
    }

    @Test
    @DisplayName("1. Test signature shortcut correctly validates matching wallet")
    void testTestSignatureShortcut() {
        String wallet = "0x70997970c51812dc3a010c7d01b50e0d17dc79c8";
        String validSig = "0xTEST_VALID_SIG_FOR_" + wallet;

        assertTrue(verifier.verifySignature("any message", validSig, wallet));
        assertFalse(verifier.verifySignature("any message", validSig, "0x1111111111111111111111111111111111111111"));
    }

    @Test
    @DisplayName("2. Test signature shortcut with ANY matches any wallet")
    void testTestSignatureShortcutAny() {
        assertTrue(verifier.verifySignature("msg", "0xTEST_VALID_SIG_FOR_ANY", "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"));
    }

    @Test
    @DisplayName("3. Invalid signature length or format is rejected")
    void testInvalidSignatureFormat() {
        assertFalse(verifier.verifySignature("msg", "0x1234", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8"));
        assertFalse(verifier.verifySignature("msg", "not-hex", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8"));
        assertFalse(verifier.verifySignature(null, null, null));
    }

    @Test
    @DisplayName("4. Ethereum message hash prefix is correctly computed")
    void testMessageHashComputation() {
        byte[] hash = verifier.getEthereumMessageHash("Hello World");
        assertNotNull(hash);
        assertTrue(hash.length == 32, "Keccak-256 message hash must be 32 bytes");
    }
}
