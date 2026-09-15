package com.ehealthshield.backend.blockchain;

import com.ehealthshield.backend.exception.CryptographicException;
import com.ehealthshield.backend.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class EthereumBlockchainService implements BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(EthereumBlockchainService.class);
    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private final EthJsonRpcClient rpcClient;

    @Value("${app.blockchain.rpc-url:http://127.0.0.1:8545}")
    private String rpcUrl = "http://127.0.0.1:8545";

    @Value("${app.blockchain.contract-address:0x5FbDB2315678afecb367f032d93F642f64180aa3}")
    private String contractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";

    @Value("${app.blockchain.chain-id:31337}")
    private long expectedChainId = 31337L;

    @Value("${app.blockchain.signer-address:0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266}")
    private String signerAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

    public EthereumBlockchainService(EthJsonRpcClient rpcClient) {
        this.rpcClient = Objects.requireNonNull(rpcClient, "EthJsonRpcClient must not be null");
    }

    @Override
    public String registerRecord(String recordId, String patientWallet, String uploaderWallet, String fileHash) {
        validateInputs(recordId, patientWallet, uploaderWallet, fileHash);
        verifyNodeAndContract();

        log.info("Anchoring EHR record {} on-chain for patient {}...", recordId, patientWallet);
        String callData = SolidityAbiEncoder.encodeRegisterRecord(recordId, patientWallet, fileHash);

        String fromAddress = (uploaderWallet != null && !uploaderWallet.isBlank()) ? uploaderWallet : signerAddress;
        String txHash = rpcClient.sendTransaction(rpcUrl, fromAddress, contractAddress, callData);
        rpcClient.waitForReceipt(rpcUrl, txHash, 10);

        log.info("EHR Record {} successfully anchored on-chain with tx hash: {}", recordId, txHash);
        return txHash;
    }

    @Override
    public String grantAccess(String recordId, String patientWallet, String doctorWallet) {
        validateInputs(recordId, patientWallet, doctorWallet);
        verifyNodeAndContract();

        log.info("Granting access on-chain for record {} to doctor {}...", recordId, doctorWallet);
        String callData = SolidityAbiEncoder.encodeGrantAccess(recordId, doctorWallet);

        String fromAddress = (patientWallet != null && !patientWallet.isBlank()) ? patientWallet : signerAddress;
        String txHash = rpcClient.sendTransaction(rpcUrl, fromAddress, contractAddress, callData);
        rpcClient.waitForReceipt(rpcUrl, txHash, 10);

        return txHash;
    }

    @Override
    public String revokeAccess(String recordId, String patientWallet, String doctorWallet) {
        validateInputs(recordId, patientWallet, doctorWallet);
        verifyNodeAndContract();

        log.info("Revoking access on-chain for record {} from doctor {}...", recordId, doctorWallet);
        String callData = SolidityAbiEncoder.encodeRevokeAccess(recordId, doctorWallet);

        String fromAddress = (patientWallet != null && !patientWallet.isBlank()) ? patientWallet : signerAddress;
        String txHash = rpcClient.sendTransaction(rpcUrl, fromAddress, contractAddress, callData);
        rpcClient.waitForReceipt(rpcUrl, txHash, 10);

        return txHash;
    }

    @Override
    public boolean checkAccess(String recordId, String accessorWallet) {
        if (recordId == null || accessorWallet == null || accessorWallet.isBlank()) {
            return false;
        }

        verifyNodeAndContract();
        String callData = SolidityAbiEncoder.encodeCheckAccess(recordId, accessorWallet);
        String hexResult = rpcClient.call(rpcUrl, contractAddress, callData);
        return SolidityAbiEncoder.decodeCheckAccess(hexResult);
    }

    @Override
    public String logSearchEvent(String searcherWallet) {
        verifyNodeAndContract();
        String callData = SolidityAbiEncoder.encodeLogSearchEvent();
        String txHash = rpcClient.sendTransaction(rpcUrl, signerAddress, contractAddress, callData);
        rpcClient.waitForReceipt(rpcUrl, txHash, 5);
        return txHash;
    }

    @Override
    public String logRetrievalEvent(String recordId, String accessorWallet) {
        verifyNodeAndContract();
        String callData = SolidityAbiEncoder.encodeLogRetrievalEvent(recordId != null ? recordId : "");
        String txHash = rpcClient.sendTransaction(rpcUrl, signerAddress, contractAddress, callData);
        rpcClient.waitForReceipt(rpcUrl, txHash, 5);
        return txHash;
    }

    @Override
    public OnChainRecordMetadata getRecordMetadata(String recordId) {
        if (recordId == null) {
            return new OnChainRecordMetadata(null, null, null, 0L, false);
        }

        verifyNodeAndContract();
        String callData = SolidityAbiEncoder.encodeGetRecord(recordId);
        String hexResult = rpcClient.call(rpcUrl, contractAddress, callData);
        return SolidityAbiEncoder.decodeGetRecord(hexResult);
    }

    private void verifyNodeAndContract() {
        if (!rpcClient.isNodeAvailable(rpcUrl)) {
            throw new CryptographicException("EVM JSON-RPC node is unreachable at " + rpcUrl);
        }
        String code = rpcClient.getCode(rpcUrl, contractAddress);
        if (code == null || code.equals("0x") || code.length() <= 2) {
            throw new CryptographicException("No deployed smart contract bytecode found at address: " + contractAddress);
        }
    }

    private void validateInputs(String recordId, String patientWallet, String doctorWallet) {
        if (recordId == null || recordId.isBlank()) {
            throw new ValidationException("Record ID cannot be blank");
        }
        if (patientWallet == null || !ETHEREUM_ADDRESS_PATTERN.matcher(patientWallet.trim()).matches()) {
            throw new ValidationException("Invalid patient wallet address format");
        }
        if (doctorWallet == null || !ETHEREUM_ADDRESS_PATTERN.matcher(doctorWallet.trim()).matches()) {
            throw new ValidationException("Invalid doctor wallet address format");
        }
    }

    private void validateInputs(String recordId, String patientWallet, String uploaderWallet, String fileHash) {
        validateInputs(recordId, patientWallet, uploaderWallet);
        if (fileHash == null || fileHash.length() != 64) {
            throw new ValidationException("Invalid SHA-256 file hash: expected 64 hex characters");
        }
    }
}
