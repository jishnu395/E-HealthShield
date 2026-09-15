package com.ehealthshield.backend.blockchain;

/**
 * Interface defining blockchain operations for E-HealthShield on Ethereum Sepolia / Hardhat.
 * Conforms to Section 17 & 19 of PROJECT_SPEC.md.
 */
public interface BlockchainService {

    /**
     * Registers an EHR record on-chain with its SHA-256 ciphertext hash anchor.
     *
     * @param recordId The unique record UUID.
     * @param patientWallet The patient owner's Ethereum address.
     * @param uploaderWallet The medical provider uploader's address.
     * @param fileHash The 64-character SHA-256 hex digest of the raw AES-GCM ciphertext.
     * @return The 66-character 0x-prefixed Ethereum transaction hash.
     */
    String registerRecord(String recordId, String patientWallet, String uploaderWallet, String fileHash);

    /**
     * Grants a doctor access rights on the smart contract ACL.
     *
     * @param recordId The unique record UUID.
     * @param patientWallet The patient owner's Ethereum address.
     * @param doctorWallet The doctor's Ethereum address to grant access.
     * @return The Ethereum transaction hash.
     */
    String grantAccess(String recordId, String patientWallet, String doctorWallet);

    /**
     * Revokes a doctor's access rights on the smart contract ACL.
     *
     * @param recordId The unique record UUID.
     * @param patientWallet The patient owner's Ethereum address.
     * @param doctorWallet The doctor's Ethereum address to revoke access.
     * @return The Ethereum transaction hash.
     */
    String revokeAccess(String recordId, String patientWallet, String doctorWallet);

    /**
     * Queries the smart contract ACL to verify if an accessor is authorized to retrieve the record.
     *
     * @param recordId The unique record UUID.
     * @param accessorWallet The accessor's Ethereum address.
     * @return true if authorized, false otherwise.
     */
    boolean checkAccess(String recordId, String accessorWallet);

    /**
     * Emits a RecordSearched event on the blockchain audit log.
     *
     * @param searcherWallet The searcher's Ethereum address.
     * @return The Ethereum transaction hash.
     */
    String logSearchEvent(String searcherWallet);

    /**
     * Emits a RecordRetrieved event on the blockchain audit log.
     *
     * @param recordId The unique record UUID.
     * @param accessorWallet The accessor's Ethereum address.
     * @return The Ethereum transaction hash.
     */
    String logRetrievalEvent(String recordId, String accessorWallet);

    /**
     * Queries on-chain anchored metadata for an EHR record.
     *
     * @param recordId The unique record UUID.
     * @return OnChainRecordMetadata or empty metadata if not registered.
     */
    OnChainRecordMetadata getRecordMetadata(String recordId);
}
