package com.ehealthshield.backend.blockchain;

import com.ehealthshield.backend.exception.CryptographicException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EthJsonRpcClientTest {

    private EthJsonRpcClient client;

    @BeforeEach
    void setUp() {
        client = new EthJsonRpcClient();
    }

    @Test
    @DisplayName("1. isNodeAvailable returns false for non-existent server endpoint")
    void testIsNodeAvailableReturnsFalseForInvalidUrl() {
        boolean available = client.isNodeAvailable("http://127.0.0.1:59999");
        assertFalse(available);
    }

    @Test
    @DisplayName("2. Communication failure throws CryptographicException with clear error message")
    void testCommunicationFailureThrowsCryptographicException() {
        CryptographicException ex = assertThrows(CryptographicException.class, () ->
                client.getChainId("http://127.0.0.1:59999"));
        assertNotNull(ex.getMessage());
    }
}
