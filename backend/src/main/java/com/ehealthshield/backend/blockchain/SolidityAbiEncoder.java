package com.ehealthshield.backend.blockchain;

import com.ehealthshield.backend.security.Web3SignatureVerifier;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Utility for ABI encoding and decoding for EHealthShieldACL smart contract calls.
 * Conforms to Ethereum ABI specification (Solidity ^0.8.20).
 */
public final class SolidityAbiEncoder {

    // Function Selectors (first 4 bytes of Keccak256 hash)
    public static final String REGISTER_RECORD_SELECTOR = getFunctionSelector("registerRecord(string,address,bytes32)");
    public static final String GRANT_ACCESS_SELECTOR = getFunctionSelector("grantAccess(string,address)");
    public static final String REVOKE_ACCESS_SELECTOR = getFunctionSelector("revokeAccess(string,address)");
    public static final String CHECK_ACCESS_SELECTOR = getFunctionSelector("checkAccess(string,address)");
    public static final String LOG_SEARCH_EVENT_SELECTOR = getFunctionSelector("logSearchEvent()");
    public static final String LOG_RETRIEVAL_EVENT_SELECTOR = getFunctionSelector("logRetrievalEvent(string)");
    public static final String GET_RECORD_SELECTOR = getFunctionSelector("getRecord(string)");

    private SolidityAbiEncoder() {
    }

    public static String getFunctionSelector(String methodSignature) {
        byte[] hash = Web3SignatureVerifier.keccak256(methodSignature.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash, 0, 4);
    }

    /**
     * Encodes registerRecord(string memory recordId, address patient, bytes32 fileHash)
     * Layout:
     * [0x00..0x04]: function selector
     * [0x04..0x24]: offset to string parameter (0x60 = 96 bytes)
     * [0x24..0x44]: address patient (left-padded 32 bytes)
     * [0x44..0x64]: bytes32 fileHash (32 bytes)
     * [0x64..0x84]: string length
     * [0x84.. ]: string content right-padded to 32-byte boundary
     */
    public static String encodeRegisterRecord(String recordId, String patientAddress, String fileHashHex) {
        StringBuilder sb = new StringBuilder("0x").append(REGISTER_RECORD_SELECTOR);

        // Parameter 1 offset: 3 words * 32 bytes = 96 = 0x60
        sb.append(padLeft(BigInteger.valueOf(96).toString(16), 64));

        // Parameter 2: address (strip 0x, left-pad to 64 hex chars)
        sb.append(padAddress(patientAddress));

        // Parameter 3: bytes32 fileHash (strip 0x, right-pad or verify 64 hex chars)
        sb.append(padBytes32(fileHashHex));

        // Dynamic String Parameter 1 content
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Encodes grantAccess(string memory recordId, address doctor)
     */
    public static String encodeGrantAccess(String recordId, String doctorAddress) {
        StringBuilder sb = new StringBuilder("0x").append(GRANT_ACCESS_SELECTOR);

        // Parameter 1 offset: 2 words * 32 bytes = 64 = 0x40
        sb.append(padLeft(BigInteger.valueOf(64).toString(16), 64));

        // Parameter 2: address
        sb.append(padAddress(doctorAddress));

        // Dynamic String Parameter 1
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Encodes revokeAccess(string memory recordId, address doctor)
     */
    public static String encodeRevokeAccess(String recordId, String doctorAddress) {
        StringBuilder sb = new StringBuilder("0x").append(REVOKE_ACCESS_SELECTOR);

        // Parameter 1 offset: 2 words * 32 bytes = 64 = 0x40
        sb.append(padLeft(BigInteger.valueOf(64).toString(16), 64));

        // Parameter 2: address
        sb.append(padAddress(doctorAddress));

        // Dynamic String Parameter 1
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Encodes checkAccess(string memory recordId, address accessor)
     */
    public static String encodeCheckAccess(String recordId, String accessorAddress) {
        StringBuilder sb = new StringBuilder("0x").append(CHECK_ACCESS_SELECTOR);

        // Parameter 1 offset: 2 words * 32 bytes = 64 = 0x40
        sb.append(padLeft(BigInteger.valueOf(64).toString(16), 64));

        // Parameter 2: address
        sb.append(padAddress(accessorAddress));

        // Dynamic String Parameter 1
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Encodes logSearchEvent()
     */
    public static String encodeLogSearchEvent() {
        return "0x" + LOG_SEARCH_EVENT_SELECTOR;
    }

    /**
     * Encodes logRetrievalEvent(string memory recordId)
     */
    public static String encodeLogRetrievalEvent(String recordId) {
        StringBuilder sb = new StringBuilder("0x").append(LOG_RETRIEVAL_EVENT_SELECTOR);

        // Parameter 1 offset: 1 word * 32 bytes = 32 = 0x20
        sb.append(padLeft(BigInteger.valueOf(32).toString(16), 64));

        // Dynamic String Parameter 1
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Encodes getRecord(string memory recordId)
     */
    public static String encodeGetRecord(String recordId) {
        StringBuilder sb = new StringBuilder("0x").append(GET_RECORD_SELECTOR);

        // Parameter 1 offset: 1 word * 32 bytes = 32 = 0x20
        sb.append(padLeft(BigInteger.valueOf(32).toString(16), 64));

        // Dynamic String Parameter 1
        sb.append(encodeDynamicString(recordId));

        return sb.toString();
    }

    /**
     * Decodes getRecord return data:
     * (bytes32 fileHash, address ownerPatient, address uploaderDoctor, uint256 timestamp, bool exists)
     */
    public static OnChainRecordMetadata decodeGetRecord(String hexData) {
        if (hexData == null || hexData.isBlank() || hexData.equals("0x") || hexData.length() < 2 + 5 * 64) {
            return new OnChainRecordMetadata(null, null, null, 0L, false);
        }

        String raw = hexData.startsWith("0x") ? hexData.substring(2) : hexData;
        if (raw.length() < 320) { // 5 * 64 chars
            return new OnChainRecordMetadata(null, null, null, 0L, false);
        }

        // Word 0: bytes32 fileHash
        String fileHash = raw.substring(0, 64).toLowerCase();

        // Word 1: address ownerPatient (last 40 hex chars of 64)
        String ownerPatient = "0x" + raw.substring(64 + 24, 128).toLowerCase();

        // Word 2: address uploaderDoctor (last 40 hex chars of 64)
        String uploaderDoctor = "0x" + raw.substring(128 + 24, 192).toLowerCase();

        // Word 3: uint256 timestamp
        BigInteger timestampBigInt = new BigInteger(raw.substring(192, 256), 16);
        long timestamp = timestampBigInt.longValue();

        // Word 4: bool exists
        BigInteger existsBigInt = new BigInteger(raw.substring(256, 320), 16);
        boolean exists = existsBigInt.intValue() == 1;

        return new OnChainRecordMetadata(fileHash, ownerPatient, uploaderDoctor, timestamp, exists);
    }

    /**
     * Decodes checkAccess boolean return data.
     */
    public static boolean decodeCheckAccess(String hexData) {
        if (hexData == null || hexData.isBlank() || hexData.equals("0x")) {
            return false;
        }
        String raw = hexData.startsWith("0x") ? hexData.substring(2) : hexData;
        if (raw.isEmpty()) {
            return false;
        }
        BigInteger result = new BigInteger(raw, 16);
        return result.intValue() == 1;
    }

    private static String encodeDynamicString(String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        int length = strBytes.length;

        String lengthHex = padLeft(BigInteger.valueOf(length).toString(16), 64);
        String dataHex = HexFormat.of().formatHex(strBytes);

        // Pad right to multiple of 32 bytes (64 hex characters)
        int remainder = dataHex.length() % 64;
        int paddingNeeded = (remainder == 0) ? 0 : (64 - remainder);
        String paddedDataHex = dataHex + "0".repeat(paddingNeeded);

        return lengthHex + paddedDataHex;
    }

    private static String padAddress(String address) {
        String clean = address.startsWith("0x") ? address.substring(2) : address;
        return padLeft(clean.toLowerCase(), 64);
    }

    private static String padBytes32(String bytes32Hex) {
        String clean = bytes32Hex.startsWith("0x") ? bytes32Hex.substring(2) : bytes32Hex;
        if (clean.length() < 64) {
            return padLeft(clean.toLowerCase(), 64);
        }
        return clean.substring(0, 64).toLowerCase();
    }

    private static String padLeft(String input, int targetLength) {
        if (input.length() >= targetLength) {
            return input;
        }
        return "0".repeat(targetLength - input.length()) + input;
    }
}
