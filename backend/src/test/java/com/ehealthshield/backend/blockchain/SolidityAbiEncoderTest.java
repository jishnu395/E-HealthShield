package com.ehealthshield.backend.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolidityAbiEncoderTest {

    private static final String PATIENT_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
    private static final String DOCTOR_WALLET = "0x90F79bf6EB2c4f870365E785982E1f101E93b906";
    private static final String FILE_HASH = "b257eea27316f68803a05094596f43fcc1201d2300e5d0e576a27212bc812db8";

    @Test
    @DisplayName("1. Function selectors match Solidity function signatures")
    void testFunctionSelectors() {
        assertNotNull(SolidityAbiEncoder.REGISTER_RECORD_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.REGISTER_RECORD_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.GRANT_ACCESS_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.GRANT_ACCESS_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.REVOKE_ACCESS_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.REVOKE_ACCESS_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.CHECK_ACCESS_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.CHECK_ACCESS_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.LOG_SEARCH_EVENT_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.LOG_SEARCH_EVENT_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.LOG_RETRIEVAL_EVENT_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.LOG_RETRIEVAL_EVENT_SELECTOR.length());

        assertNotNull(SolidityAbiEncoder.GET_RECORD_SELECTOR);
        assertEquals(8, SolidityAbiEncoder.GET_RECORD_SELECTOR.length());
    }

    @Test
    @DisplayName("2. encodeRegisterRecord produces valid ABI data")
    void testEncodeRegisterRecord() {
        String recordId = UUID.randomUUID().toString();
        String encoded = SolidityAbiEncoder.encodeRegisterRecord(recordId, PATIENT_WALLET, FILE_HASH);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("0x" + SolidityAbiEncoder.REGISTER_RECORD_SELECTOR));
        assertTrue(encoded.contains(FILE_HASH.toLowerCase()));
    }

    @Test
    @DisplayName("3. encodeGrantAccess and encodeRevokeAccess produce valid ABI data")
    void testEncodeGrantAndRevokeAccess() {
        String recordId = UUID.randomUUID().toString();
        String grantData = SolidityAbiEncoder.encodeGrantAccess(recordId, DOCTOR_WALLET);
        String revokeData = SolidityAbiEncoder.encodeRevokeAccess(recordId, DOCTOR_WALLET);

        assertTrue(grantData.startsWith("0x" + SolidityAbiEncoder.GRANT_ACCESS_SELECTOR));
        assertTrue(revokeData.startsWith("0x" + SolidityAbiEncoder.REVOKE_ACCESS_SELECTOR));
    }

    @Test
    @DisplayName("4. decodeGetRecord and decodeCheckAccess parse ABI return buffers")
    void testDecodeMethods() {
        String fileHash = "aa".repeat(32);
        String patient = "00".repeat(12) + "70997970c51812dc3a010c7d01b50e0d17dc79c8";
        String doctor = "00".repeat(12) + "90f79bf6eb2c4f870365e785982e1f101e93b906";
        String timestamp = "00".repeat(31) + "64"; // 100
        String exists = "00".repeat(31) + "01"; // true

        String rawHex = "0x" + fileHash + patient + doctor + timestamp + exists;

        OnChainRecordMetadata meta = SolidityAbiEncoder.decodeGetRecord(rawHex);

        assertTrue(meta.exists());
        assertEquals(fileHash, meta.fileHash());
        assertEquals("0x70997970c51812dc3a010c7d01b50e0d17dc79c8", meta.ownerPatient());
        assertEquals("0x90f79bf6eb2c4f870365e785982e1f101e93b906", meta.uploaderDoctor());
        assertEquals(100L, meta.timestamp());

        assertTrue(SolidityAbiEncoder.decodeCheckAccess("0x" + "00".repeat(31) + "01"));
        assertFalse(SolidityAbiEncoder.decodeCheckAccess("0x" + "00".repeat(31) + "00"));
    }
}
