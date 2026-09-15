package com.ehealthshield.backend.blockchain;

/**
 * On-chain record metadata anchored in the Ethereum smart contract.
 */
public record OnChainRecordMetadata(
        String fileHash,
        String ownerPatient,
        String uploaderDoctor,
        long timestamp,
        boolean exists
) {
}
