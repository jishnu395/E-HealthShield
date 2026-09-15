package com.ehealthshield.backend.blockchain;

import com.ehealthshield.backend.exception.CryptographicException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Standard JSON-RPC 2.0 Client for Ethereum EVM nodes (Hardhat, Ganache, Sepolia, etc.).
 */
@Component
public class EthJsonRpcClient {

    private static final Logger log = LoggerFactory.getLogger(EthJsonRpcClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestId = new AtomicLong(1);

    public EthJsonRpcClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks if the Ethereum JSON-RPC node is reachable.
     */
    public boolean isNodeAvailable(String rpcUrl) {
        try {
            JsonNode result = executeRpc(rpcUrl, "eth_blockNumber", objectMapper.createArrayNode());
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Queries the network Chain ID.
     */
    public long getChainId(String rpcUrl) {
        JsonNode result = executeRpc(rpcUrl, "eth_chainId", objectMapper.createArrayNode());
        if (result == null || !result.isTextual()) {
            throw new CryptographicException("Failed to retrieve chain ID from EVM node");
        }
        String hex = result.asText();
        return Long.decode(hex);
    }

    /**
     * Queries deployed bytecode at a contract address.
     */
    public String getCode(String rpcUrl, String contractAddress) {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(contractAddress);
        params.add("latest");

        JsonNode result = executeRpc(rpcUrl, "eth_getCode", params);
        if (result == null || !result.isTextual()) {
            return "0x";
        }
        return result.asText();
    }

    /**
     * Submits an Ethereum transaction via eth_sendTransaction from a configured/unlocked signer.
     */
    public String sendTransaction(String rpcUrl, String from, String to, String data) {
        ObjectNode txObj = objectMapper.createObjectNode();
        txObj.put("from", from);
        txObj.put("to", to);
        txObj.put("data", data);
        txObj.put("gas", "0x2DC6C0"); // 3,000,000 gas limit

        ArrayNode params = objectMapper.createArrayNode();
        params.add(txObj);

        JsonNode result = executeRpc(rpcUrl, "eth_sendTransaction", params);
        if (result == null || !result.isTextual()) {
            throw new CryptographicException("eth_sendTransaction failed to return transaction hash");
        }
        return result.asText();
    }

    /**
     * Executes a read-only smart contract call via eth_call.
     */
    public String call(String rpcUrl, String to, String data) {
        ObjectNode callObj = objectMapper.createObjectNode();
        callObj.put("to", to);
        callObj.put("data", data);

        ArrayNode params = objectMapper.createArrayNode();
        params.add(callObj);
        params.add("latest");

        JsonNode result = executeRpc(rpcUrl, "eth_call", params);
        if (result == null || !result.isTextual()) {
            return "0x";
        }
        return result.asText();
    }

    /**
     * Queries a transaction receipt via eth_getTransactionReceipt.
     */
    public JsonNode getTransactionReceipt(String rpcUrl, String txHash) {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(txHash);
        return executeRpc(rpcUrl, "eth_getTransactionReceipt", params);
    }

    /**
     * Polls until the transaction is mined and validates that status == "0x1" (success).
     */
    public JsonNode waitForReceipt(String rpcUrl, String txHash, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            JsonNode receipt = getTransactionReceipt(rpcUrl, txHash);
            if (receipt != null && !receipt.isNull() && receipt.has("status")) {
                String status = receipt.get("status").asText();
                if ("0x1".equals(status) || "1".equals(status)) {
                    log.info("EVM Transaction mined successfully: {} in block {}", txHash, receipt.path("blockNumber").asText());
                    return receipt;
                } else if ("0x0".equals(status) || "0".equals(status)) {
                    throw new CryptographicException("EVM Transaction reverted on-chain: " + txHash);
                }
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CryptographicException("Interrupted while awaiting EVM receipt", e);
            }
        }

        throw new CryptographicException("Timeout waiting for EVM transaction receipt: " + txHash);
    }

    private JsonNode executeRpc(String rpcUrl, String method, ArrayNode params) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("jsonrpc", "2.0");
            requestBody.put("method", method);
            requestBody.set("params", params);
            requestBody.put("id", requestId.incrementAndGet());

            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(rpcUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("RPC Error HTTP {}: {}", response.statusCode(), response.body());
                throw new CryptographicException("EVM RPC node returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error") && !root.get("error").isNull()) {
                String errorMsg = root.get("error").path("message").asText();
                log.error("RPC Protocol Error for method {}: {}", method, errorMsg);
                throw new CryptographicException("EVM RPC Error [" + method + "]: " + errorMsg);
            }

            return root.get("result");
        } catch (CryptographicException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("RPC communication failure with {}: {}", rpcUrl, e.getMessage());
            throw new CryptographicException("Failed to communicate with EVM node at " + rpcUrl + ": " + e.getMessage(), e);
        }
    }
}
