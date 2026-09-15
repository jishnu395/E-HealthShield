// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title EHealthShieldACL
 * @dev Quantum-Secure and Blockchain-Auditable Framework for Electronic Health Records
 * Conforms to Section 17 & 19 of PROJECT_SPEC.md.
 */
contract EHealthShieldACL {

    struct RecordMetadata {
        bytes32 fileHash;
        address ownerPatient;
        address uploaderDoctor;
        uint256 timestamp;
        bool exists;
    }

    // Mapping: recordId (UUID string) => RecordMetadata
    mapping(string => RecordMetadata) private records;

    // Mapping: recordId => (accessorAddress => isAuthorized)
    mapping(string => mapping(address => bool)) private accessPermissions;

    // Events for immutable on-chain audit trail
    event RecordUploaded(
        string indexed recordId,
        address indexed uploader,
        address indexed patient,
        bytes32 fileHash,
        uint256 timestamp
    );

    event AccessGranted(
        string indexed recordId,
        address indexed patient,
        address indexed doctor,
        uint256 timestamp
    );

    event AccessRevoked(
        string indexed recordId,
        address indexed patient,
        address indexed doctor,
        uint256 timestamp
    );

    event RecordSearched(
        address indexed searcher,
        uint256 timestamp
    );

    event RecordRetrieved(
        string indexed recordId,
        address indexed accessor,
        uint256 timestamp
    );

    modifier onlyRecordOwner(string memory recordId) {
        require(records[recordId].exists, "Record does not exist");
        require(records[recordId].ownerPatient == msg.sender, "Caller is not record patient owner");
        _;
    }

    /**
     * @dev Registers a new EHR record with its SHA-256 ciphertext hash anchor.
     */
    function registerRecord(
        string memory recordId,
        address patient,
        bytes32 fileHash
    ) external {
        require(bytes(recordId).length > 0, "Record ID cannot be empty");
        require(patient != address(0), "Invalid patient address");
        require(fileHash != bytes32(0), "File hash cannot be zero");
        require(!records[recordId].exists, "Record already registered");

        records[recordId] = RecordMetadata({
            fileHash: fileHash,
            ownerPatient: patient,
            uploaderDoctor: msg.sender,
            timestamp: block.timestamp,
            exists: true
        });

        emit RecordUploaded(recordId, msg.sender, patient, fileHash, block.timestamp);
    }

    /**
     * @dev Grants a doctor access to an EHR record. Only callable by patient owner.
     */
    function grantAccess(string memory recordId, address doctor) external onlyRecordOwner(recordId) {
        require(doctor != address(0), "Invalid doctor address");
        require(doctor != msg.sender, "Cannot grant access to self");

        accessPermissions[recordId][doctor] = true;

        emit AccessGranted(recordId, msg.sender, doctor, block.timestamp);
    }

    /**
     * @dev Revokes a doctor's access to an EHR record. Only callable by patient owner.
     */
    function revokeAccess(string memory recordId, address doctor) external onlyRecordOwner(recordId) {
        require(doctor != address(0), "Invalid doctor address");

        accessPermissions[recordId][doctor] = false;

        emit AccessRevoked(recordId, msg.sender, doctor, block.timestamp);
    }

    /**
     * @dev Evaluates whether an accessor address is authorized to retrieve the EHR record.
     */
    function checkAccess(string memory recordId, address accessor) external view returns (bool) {
        if (!records[recordId].exists || accessor == address(0)) {
            return false;
        }

        RecordMetadata memory record = records[recordId];

        // 1. Patient Owner has full access
        if (record.ownerPatient == accessor) {
            return true;
        }

        // 2. Original Uploader Doctor has access
        if (record.uploaderDoctor == accessor) {
            return true;
        }

        // 3. Explicit Access Permission Grant
        return accessPermissions[recordId][accessor];
    }

    /**
     * @dev Logs a blind search event to the immutable blockchain audit trail.
     */
    function logSearchEvent() external {
        emit RecordSearched(msg.sender, block.timestamp);
    }

    /**
     * @dev Logs an EHR record retrieval event to the immutable blockchain audit trail.
     */
    function logRetrievalEvent(string memory recordId) external {
        require(records[recordId].exists, "Record does not exist");
        emit RecordRetrieved(recordId, msg.sender, block.timestamp);
    }

    /**
     * @dev Queries on-chain metadata and SHA-256 file hash anchor for an EHR record.
     */
    function getRecord(string memory recordId) external view returns (
        bytes32 fileHash,
        address ownerPatient,
        address uploaderDoctor,
        uint256 timestamp,
        bool exists
    ) {
        RecordMetadata memory record = records[recordId];
        return (
            record.fileHash,
            record.ownerPatient,
            record.uploaderDoctor,
            record.timestamp,
            record.exists
        );
    }
}
