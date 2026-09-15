package com.ehealthshield.backend.blockchain;

import com.ehealthshield.backend.exception.CryptographicException;
import com.ehealthshield.backend.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EthereumBlockchainServiceTest {

    @Mock
    private EthJsonRpcClient rpcClient;

    private EthereumBlockchainService blockchainService;

    private static final String PATIENT_WALLET = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
    private static final String DOCTOR_WALLET = "0x90F79bf6EB2c4f870365E785982E1f101E93b906";

    private String recordId;
    private String fileHash;
    private String mockTxHash;

    @BeforeEach
    void setUp() {
        blockchainService = new EthereumBlockchainService(rpcClient);
        recordId = UUID.randomUUID().toString();
        fileHash = "aabbccdd" + "11223344".repeat(7);
        mockTxHash = "0x" + "11".repeat(32);
    }

    @Test
    @DisplayName("1. Successful registerRecord dispatches EVM transaction and waits for receipt")
    void testRegisterRecordSuccess() {
        when(rpcClient.isNodeAvailable(anyString())).thenReturn(true);
        when(rpcClient.getCode(anyString(), anyString())).thenReturn("0x60806040...");
        when(rpcClient.sendTransaction(anyString(), anyString(), anyString(), anyString())).thenReturn(mockTxHash);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode receipt = mapper.createObjectNode();
        receipt.put("status", "0x1");
        receipt.put("blockNumber", "0x10");
        when(rpcClient.waitForReceipt(anyString(), anyString(), anyInt())).thenReturn(receipt);

        String txHash = blockchainService.registerRecord(recordId, PATIENT_WALLET, DOCTOR_WALLET, fileHash);

        assertNotNull(txHash);
        assertEquals(mockTxHash, txHash);
        verify(rpcClient).sendTransaction(anyString(), anyString(), anyString(), anyString());
        verify(rpcClient).waitForReceipt(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("2. Unreachable RPC node throws CryptographicException (no fake hash returned)")
    void testUnreachableRpcNodeThrowsException() {
        when(rpcClient.isNodeAvailable(anyString())).thenReturn(false);

        CryptographicException ex = assertThrows(CryptographicException.class, () ->
                blockchainService.registerRecord(recordId, PATIENT_WALLET, DOCTOR_WALLET, fileHash));

        assertTrue(ex.getMessage().contains("EVM JSON-RPC node is unreachable"));
    }

    @Test
    @DisplayName("3. Missing contract bytecode throws CryptographicException")
    void testMissingContractBytecodeThrowsException() {
        when(rpcClient.isNodeAvailable(anyString())).thenReturn(true);
        when(rpcClient.getCode(anyString(), anyString())).thenReturn("0x");

        CryptographicException ex = assertThrows(CryptographicException.class, () ->
                blockchainService.registerRecord(recordId, PATIENT_WALLET, DOCTOR_WALLET, fileHash));

        assertTrue(ex.getMessage().contains("No deployed smart contract bytecode found"));
    }

    @Test
    @DisplayName("4. Reverted EVM transaction propagates receipt exception")
    void testRevertedTransactionThrowsException() {
        when(rpcClient.isNodeAvailable(anyString())).thenReturn(true);
        when(rpcClient.getCode(anyString(), anyString())).thenReturn("0x60806040...");
        when(rpcClient.sendTransaction(anyString(), anyString(), anyString(), anyString())).thenReturn(mockTxHash);
        when(rpcClient.waitForReceipt(anyString(), anyString(), anyInt()))
                .thenThrow(new CryptographicException("EVM Transaction reverted on-chain: " + mockTxHash));

        CryptographicException ex = assertThrows(CryptographicException.class, () ->
                blockchainService.registerRecord(recordId, PATIENT_WALLET, DOCTOR_WALLET, fileHash));

        assertTrue(ex.getMessage().contains("reverted on-chain"));
    }

    @Test
    @DisplayName("5. Invalid inputs throw ValidationException")
    void testInvalidInputsThrowValidationException() {
        assertThrows(ValidationException.class, () ->
                blockchainService.registerRecord("", PATIENT_WALLET, DOCTOR_WALLET, fileHash));

        assertThrows(ValidationException.class, () ->
                blockchainService.registerRecord(recordId, "0xINVALID", DOCTOR_WALLET, fileHash));

        assertThrows(ValidationException.class, () ->
                blockchainService.registerRecord(recordId, PATIENT_WALLET, DOCTOR_WALLET, "short"));
    }

    @Test
    @DisplayName("6. Read operations checkAccess and getRecordMetadata call EVM eth_call")
    void testReadOperations() {
        when(rpcClient.isNodeAvailable(anyString())).thenReturn(true);
        when(rpcClient.getCode(anyString(), anyString())).thenReturn("0x60806040...");

        // checkAccess
        when(rpcClient.call(anyString(), anyString(), anyString())).thenReturn("0x" + "00".repeat(31) + "01");
        boolean hasAccess = blockchainService.checkAccess(recordId, PATIENT_WALLET);
        assertTrue(hasAccess);

        // getRecordMetadata
        String fileHashHex = "aa".repeat(32);
        String patientHex = "00".repeat(12) + PATIENT_WALLET.substring(2).toLowerCase();
        String doctorHex = "00".repeat(12) + DOCTOR_WALLET.substring(2).toLowerCase();
        String timestampHex = "00".repeat(31) + "64";
        String existsHex = "00".repeat(31) + "01";

        when(rpcClient.call(anyString(), anyString(), anyString())).thenReturn("0x" + fileHashHex + patientHex + doctorHex + timestampHex + existsHex);
        OnChainRecordMetadata meta = blockchainService.getRecordMetadata(recordId);

        assertTrue(meta.exists());
        assertEquals(fileHashHex, meta.fileHash());
        assertEquals(PATIENT_WALLET.toLowerCase(), meta.ownerPatient());
    }
}
