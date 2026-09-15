# E-HealthShield: Project Technical Specification

> **Document Type:** Authoritative Technical Specification
> **Source Reference:** Phase-1 Project Report — *E-HealthShield: A Quantum-Secure and Blockchain-Auditable Framework for Electronic Health Records* (Dept. of ICBT, SMVIT / VTU, 2025–2026)
> **Backend Implementation Stack:** Java + Spring Boot + REST API + PostgreSQL
> **Blockchain & Frontend Stack:** Ethereum Sepolia Testnet + Solidity + React.js + Ethers.js + MetaMask
> **Cryptographic Stack:** CRYSTALS-Kyber (ML-KEM, NIST FIPS 203) + AES-256 + SSE (HMAC-SHA256) + SHA-256

---

## 1. Project Overview

**E-HealthShield** is a decentralized, quantum-secure, and blockchain-auditable framework designed for managing, storing, and searching Electronic Health Records (EHRs). It addresses the systemic vulnerabilities of centralized legacy medical databases by integrating Post-Quantum Cryptography (PQC), Searchable Symmetric Encryption (SSE), and Ethereum Smart Contracts into a unified full-stack architecture.

The system ensures that patient medical data remains quantum-safe against future decryption attacks, enables blind keyword search over encrypted data without exposing plaintext to the cloud storage layer, and enforces tamper-proof, transparent access governance with immutable audit logging on a public blockchain ledger. The project contributes to **UN Sustainable Development Goal 3 (Good Health and Well-being)** and **Goal 16 (Peace, Justice and Strong Institutions)**.

---

## 2. Problems Being Addressed

### Problem 1: Quantum Vulnerability of Current Encryption Standards
- Existing EHR frameworks rely almost exclusively on classical asymmetric cryptography (RSA, ECC / ECDSA, Diffie-Hellman), whose mathematical security rests on the computational hardness of integer factorization and discrete logarithms.
- Shor's Algorithm running on a sufficiently capable quantum computer solves these mathematical problems in polynomial time, rendering all classical public-key encryption obsolete.
- **"Harvest Now, Decrypt Later" (HNDL)** attacks are actively occurring: adversaries intercept and store encrypted healthcare records today to decrypt them once cryptographically relevant quantum computers become available.
- Healthcare records contain permanent, immutable biological and biographical information (dates of birth, genetic profiles, chronic illness histories, insurance identifiers) that cannot be revoked or reissued like compromised credit cards.
- Between 2009 and 2023, approximately 81% of American healthcare records were compromised in data breaches, with the healthcare sector experiencing over 1,426 cyberattacks per week in 2022.

### Problem 2: The Searchability Trade-off in Encrypted Databases
- Traditional database encryption creates a severe operational trade-off: fully encrypting EHR files protects patient confidentiality but renders records completely unsearchable.
- To find a single record in a conventionally encrypted database, the entire dataset must be downloaded and decrypted locally, creating unacceptable computational overhead and latency in time-critical clinical emergencies.
- Existing healthcare applications are forced to choose between storing searchable plaintext (sacrificing privacy) or unsearchable ciphertext (sacrificing clinical usability).

### Problem 3: Lack of Cryptographically Guaranteed Audit Integrity
- Conventional healthcare systems rely on centralized relational database logs to track access events.
- Centralized logs are inherently mutable and vulnerable: system administrators, malicious insiders, or external attackers who compromise privileged credentials can alter, delete, or fabricate audit entries without detection.
- Centralized logging fails to meet the strict compliance and verification mandates of modern data privacy regulations such as HIPAA and GDPR, which require non-repudiable, mathematically verifiable access accounting.

---

## 3. Scope

- **In Scope:**
  - Engineering a functional full-stack research prototype demonstrating the real-world integration of NIST-standard post-quantum cryptography, searchable encryption, and blockchain technology.
  - Implementation of CRYSTALS-Kyber (ML-KEM, NIST FIPS 203) for quantum-resistant asymmetric key encapsulation.
  - Implementation of AES-256 for symmetric document encryption in a hybrid cryptographic scheme.
  - Implementation of Searchable Symmetric Encryption (SSE) utilizing HMAC-SHA256 for blind keyword matching over encrypted storage.
  - Deployment of Solidity Smart Contracts on the Ethereum Sepolia testnet to enforce an immutable Access Control List (ACL) and maintain an on-chain audit trail.
  - Development of a secure Web3-enabled React.js frontend with MetaMask wallet-based authentication.
  - Implementation of a high-performance Java + Spring Boot REST API backend managing cryptographic orchestration, database persistence, and blockchain connectivity.
  - Configuration of PostgreSQL as a zero-knowledge encrypted database repository storing exclusively ciphertexts, search tags, and encapsulated key bundles.

- **Out of Scope (Future Scope):**
  - Full-scale multi-center clinical hospital trials and long-term longitudinal validation.
  - Formal regulatory compliance audits and legal certification (e.g., FDA/HIPAA production certification).
  - Cross-chain interoperability with private/consortium blockchains.
  - Automated natural language processing (NLP) for unstructured clinical notes parsing at scale.

---

## 4. Functional Objectives

1. **Web3 Authentication:** Provide passwordless, cryptographic identity verification for all users via MetaMask wallet signatures.
2. **Role-Based Workspaces:** Deliver customized, intuitive dashboards tailored specifically for **Patient** and **Doctor** roles.
3. **EHR Document Upload & Ingestion:** Enable doctors to upload EHR files, automatically extract document keywords/metadata, generate searchable tags, and encrypt payloads before persistence.
4. **Blind Keyword Search:** Enable authorized users to search stored records by keyword using cryptographic trapdoors without decrypting the database or exposing search terms.
5. **Decentralized Access Authorization:** Evaluate user access permissions dynamically against on-chain Solidity smart contract ACLs prior to record retrieval.
6. **Zero-Knowledge Storage:** Persist only AES-256 ciphertext bundles, Kyber-encapsulated key bundles, and HMAC-SHA256 search tags in PostgreSQL.
7. **End-to-End Cryptographic Integrity Verification:** Recompute SHA-256 ciphertext hashes upon retrieval and compare them against immutable on-chain hashes.
8. **Client-Side Document Decryption:** Enable patients/authorized users to unwrap encapsulated AES keys using their private Kyber key and decrypt medical records locally.
9. **On-Chain Audit Logging:** Automatically record every document upload, search query, and retrieval event as an immutable Ethereum blockchain transaction.

---

## 5. Security Objectives

1. **Quantum Resistance:** Guarantee confidentiality against both classical computers and future quantum computing attacks running Shor's algorithm.
2. **Zero-Knowledge Cloud Environment:** Ensure the database server and backend application layers have zero visibility into plaintext medical contents, extracted keywords, or search queries.
3. **Immutable Audit Integrity:** Establish mathematical non-repudiation for all record interactions so logs cannot be deleted, modified, or forged by any party (including system administrators).
4. **Cryptographic Confidentiality:** Ensure bulk medical data is protected with military-grade AES-256 encryption.
5. **Verifiable Data Integrity:** Guarantee instantaneous detection of any unauthorized database alteration or ciphertext corruption using SHA-256 hash anchoring on Ethereum.
6. **Decentralized Principle of Least Privilege:** Strictly enforce role-based access rights at the smart contract level, eliminating centralized single points of compromise.

---

## 6. Required Technologies

| Component / Layer | Technology Selected | Description / Purpose |
|---|---|---|
| **Backend Framework** | **Java 17+ / Spring Boot 3.x** | Enterprise REST API backend, cryptographic orchestration, business logic, security filters |
| **Database / Storage** | **PostgreSQL 15+** | Relational database operating as a zero-knowledge store for ciphertexts, SSE tags, and key bundles |
| **Post-Quantum Cryptography** | **CRYSTALS-Kyber (ML-KEM / FIPS 203)** | NIST-standardized lattice-based Key Encapsulation Mechanism for quantum-safe key wrapping |
| **Symmetric Cryptography** | **AES-256 (GCM / CBC)** | High-throughput symmetric encryption for bulk EHR documents and clinical payloads |
| **Searchable Cryptography** | **SSE via HMAC-SHA256** | Searchable Symmetric Encryption producing blind keyword search tags and query trapdoors |
| **Integrity Hashing** | **SHA-256** | Cryptographic digest generation for on-chain anchoring and file tamper verification |
| **Blockchain Network** | **Ethereum Sepolia Testnet** | Public decentralized testnet for smart contract execution and immutable transaction logging |
| **Smart Contracts** | **Solidity (v0.8.x)** | On-chain Access Control List (ACL) logic and tamper-proof event auditing |
| **Web3 Client Integration** | **Ethers.js / Web3j** | JavaScript / Java client libraries for Ethereum blockchain communication |
| **Frontend Framework** | **React.js (v18+)** | Modern single-page user interface with reactive state management |
| **Authentication Provider** | **MetaMask Wallet** | Non-custodial Web3 wallet enabling cryptographic signature-based authentication |

---

## 7. System Architecture

The E-HealthShield framework is structured into a four-tier decentralized architecture:

```
+-----------------------------------------------------------------------------------+
|                              1. FRONTEND LAYER (React.js)                         |
|  - MetaMask Web3 Authentication (Signature Verification)                           |
|  - Patient Dashboard (Record View, Audit Logs, Local Decryption)                  |
|  - Doctor Dashboard (Record Upload, Keyword Search, Access Request)               |
|  - Ethers.js Blockchain RPC Connection                                            |
+------------------------------------------+----------------------------------------+
                                           | HTTP REST / Web3 RPC
                                           v
+-----------------------------------------------------------------------------------+
|                        2. BACKEND LAYER (Java + Spring Boot)                      |
|  - REST API Controllers (/api/ehr, /api/search, /api/auth, /api/audit)             |
|  - Hybrid Cryptography Engine (AES-256 + ML-KEM Key Encapsulation)               |
|  - SSE Module (HMAC-SHA256 Keyword Tagging & Trapdoor Processing)                 |
|  - Integrity Verification Engine (SHA-256 Hashing)                                |
|  - Web3j / Ethereum Blockchain Service Intermediary                              |
+---------------------+---------------------------------------+---------------------+
                      | SQL (Ciphertexts & Tags)              | Web3 Transactions
                      v                                       v
+------------------------------------+  +-------------------------------------------+
|      3. ZERO-KNOWLEDGE STORAGE     |  |          4. BLOCKCHAIN LAYER              |
|        (PostgreSQL Database)       |  |       (Ethereum Sepolia Testnet)          |
|  - AES-256 Encrypted EHR Payloads  |  |  - Solidity Smart Contract                |
|  - ML-KEM Encapsulated Key Bundles |  |  - Immutable Access Control List (ACL)    |
|  - HMAC-SHA256 SSE Search Tags     |  |  - On-Chain Event Audit Trail             |
|  - Patient / Record Identifier Map |  |  - SHA-256 Ciphertext Integrity Hashes    |
+------------------------------------+  +-------------------------------------------+
```

### Layer Details:
1. **Frontend Layer (React.js):** Provides role-based user interfaces for Patients and Doctors, interacts with MetaMask for wallet signing, calls backend REST APIs, and conducts client-side key unwrapping and decryption.
2. **Backend Layer (Java + Spring Boot):** Exposes secure REST endpoints, coordinates cryptographic workflows (AES-256 encryption/decryption, ML-KEM encapsulation, SSE tag and trapdoor generation), and interacts with PostgreSQL and Ethereum.
3. **Storage Layer (PostgreSQL):** Functions strictly as a zero-knowledge ciphertext repository; never receives or stores plaintext medical information.
4. **Blockchain Layer (Ethereum Sepolia + Solidity):** Manages access permissions on-chain via smart contracts and logs permanent audit records containing actor wallet addresses, action types, timestamps, and record hashes.

---

## 8. User Roles and Responsibilities

### Patient (Data Owner)
- **Identity:** Identified by a unique Ethereum wallet address.
- **Keys:** Holds a CRYSTALS-Kyber public/private keypair. The public key is published for encryption; the private key is maintained securely on the client device for decryption.
- **Permissions & Capabilities:**
  - Grants or revokes access permissions to specific doctors on the blockchain ACL.
  - Inspects the complete, immutable audit trail of all access events involving their records.
  - Downloads and decrypts their own EHR records locally using their Kyber private key.

### Doctor (Authorized Healthcare Provider)
- **Identity:** Authenticated via verified Ethereum wallet address.
- **Permissions & Capabilities:**
  - Uploads new patient EHR records (extracts clinical keywords, initiates hybrid encryption).
  - Performs blind keyword searches across the encrypted database via backend trapdoor queries.
  - Requests retrieval of specific patient EHR files subject to smart contract ACL verification.

---

## 9. EHR Upload Workflow

The upload workflow transforms a plaintext clinical record into a quantum-encrypted bundle, indexes it blindly, and anchors it on the blockchain through the following 6-step cryptographic pipeline:

```
[Doctor/Frontend] --- 1. Select EHR File & Extract Keywords ---> [Spring Boot Backend]
                                                                        |
                                         +------------------------------+------------------------------+
                                         |                              |                              |
                                2. SSE Tag Generation          3. AES File Encryption        4. Key Encapsulation
                                   HMAC-SHA256(K_sse, w)          AES-256-GCM(K_aes, File)      ML-KEM.Encaps(PK_patient)
                                         |                              |                              |
                                         +------------------------------+------------------------------+
                                                                        |
                                           5. Store Bundle (Ciphertext, Tags, Encapsulated Key)
                                                                        v
                                                               [PostgreSQL Storage]
                                                                        |
                                           6. Log Upload & Anchor SHA-256(Ciphertext)
                                                                        v
                                                            [Ethereum Smart Contract]
```

1. **Upload & Keyword Extraction:** The doctor submits the patient's EHR file via the React frontend. Key clinical descriptors (e.g., diagnoses, medication codes, dates) are extracted from document metadata and content.
2. **SSE Search Tag Generation:** Each extracted keyword $w_i$ is hashed using HMAC-SHA256 parameterized with the backend master SSE key $K_{\text{SSE}}$:
   $$\text{Tag}_i = \text{HMAC-SHA256}(K_{\text{SSE}}, w_i)$$
3. **Symmetric Payload Encryption:** A unique 256-bit symmetric key $K_{\text{AES}}$ is generated. The EHR document is encrypted using AES-256 to produce the ciphertext bundle $C_{\text{EHR}}$.
4. **Quantum-Resistant Key Encapsulation:** The symmetric key $K_{\text{AES}}$ is encapsulated using the patient's CRYSTALS-Kyber public key $PK_{\text{Patient}}$:
   $$(C_{\text{Key}}, K_{\text{Shared}}) = \text{ML-KEM.Encaps}(PK_{\text{Patient}})$$
   The AES key is protected under the derived quantum-safe shared secret.
5. **Zero-Knowledge Storage Persistence:** The backend stores the encrypted payload ($C_{\text{EHR}}$), the encapsulated key bundle ($C_{\text{Key}}$), and the array of search tags $\{\text{Tag}_1, \dots, \text{Tag}_n\}$ in PostgreSQL.
6. **Blockchain Anchoring & Audit Logging:** The backend computes the SHA-256 hash of $C_{\text{EHR}}$ and issues an on-chain transaction calling the Solidity smart contract to store the record hash and log the `RecordUploaded` audit event.

---

## 10. Search Workflow

The search workflow allows authorized providers to query records blindly without revealing search terms or document contents to the storage server:

```
[User/Frontend] --- 1. Enters Search Keyword (w) ---> [Spring Boot Backend]
                                                             |
                                                  2. Compute Trapdoor
                                                     T_w = HMAC-SHA256(K_sse, w)
                                                             |
                                                  3. Match Trapdoor against Tags
                                                             v
                                                    [PostgreSQL Storage]
                                                             |
                                                  4. Return Matching Record IDs
                                                             v
                                                  [Doctor Search Results]
```

1. **Keyword Input:** An authorized user enters a search term $w$ in the frontend search interface.
2. **Trapdoor Computation:** The Spring Boot backend computes the cryptographic search trapdoor $T_w$ using the master SSE key:
   $$T_w = \text{HMAC-SHA256}(K_{\text{SSE}}, w)$$
3. **Blind Tag Matching:** The backend executes a parameterized SQL query in PostgreSQL matching $T_w$ against the stored `search_tags` index table:
   ```sql
   SELECT DISTINCT record_id FROM record_search_tags WHERE search_tag = ?;
   ```
4. **Result Delivery:** Matching record identifiers and encrypted metadata are returned to the user interface. At no point does the database engine or storage server learn the plaintext keyword or file content.

---

## 11. Retrieval Workflow

The retrieval workflow enforces smart contract access validation, cryptographic file integrity verification, and client-side quantum key unwrapping:

```
[Doctor/Frontend] --- 1. Request Record Retrieval (Record ID, Wallet) ---> [Spring Boot Backend]
                                                                                  |
                                                           2. Verify ACL on Smart Contract
                                                              (Ethereum Sepolia Testnet)
                                                                                  |
                                                           3. Fetch Ciphertext from PostgreSQL
                                                                                  |
                                                           4. Verify SHA-256 Hash vs Blockchain Hash
                                                                                  |
                                                           5. Deliver Encrypted Bundle to Frontend
                                                                                  |
                                                           6. Client Decrypts:
                                                              - ML-KEM.Decaps(SK_patient, C_Key) -> K_aes
                                                              - AES-256-Decrypt(K_aes, C_EHR) -> Plaintext EHR
                                                                                  |
                                                           7. Log Retrieval Event on Blockchain
```

1. **Access Request:** The user selects a record from the search results and initiates a retrieval request signed by their MetaMask wallet.
2. **On-Chain ACL Verification:** The backend queries the Solidity smart contract (`checkAccess(recordId, requesterAddress)`). If the requester is not the record owner or an authorized doctor, access is rejected immediately.
3. **Ciphertext Fetching:** Upon successful ACL validation, the backend retrieves $C_{\text{EHR}}$ and $C_{\text{Key}}$ from PostgreSQL.
4. **Data Integrity Verification:** The backend recomputes the SHA-256 hash of $C_{\text{EHR}}$ and verifies that it exactly matches the hash permanently stored on the Ethereum smart contract. If a mismatch is detected, execution aborts with a tamper alert.
5. **Encrypted Bundle Transmission:** The verified ciphertext and encapsulated key bundle are delivered to the client application.
6. **Local Key Decapsulation & Decryption:**
   - The patient's client un-encapsulates the AES key using the private Kyber key $SK_{\text{Patient}}$:
     $$K_{\text{Shared}} = \text{ML-KEM.Decaps}(SK_{\text{Patient}}, C_{\text{Key}})$$
   - The symmetric key is recovered and used to decrypt $C_{\text{EHR}}$ via AES-256 on the local device.
7. **Audit Logging:** The smart contract emits an on-chain `RecordRetrieved` transaction recording the accessor's address, record ID, and timestamp.

---

## 12. ML-KEM Requirements

- **Standardization:** Must conform to **NIST FIPS 203 (2024)** — Module-Lattice-Based Key Encapsulation Mechanism Standard (derived from CRYSTALS-Kyber).
- **Security Foundation:** Mathematical hardness based on the Learning With Errors over Module Lattices (M-LWE) problem, providing provable resistance against polynomial-time quantum algorithms (Shor's and Grover's algorithms).
- **Supported Parameter Sets:**
  - `ML-KEM-512` (NIST Security Category 1, equivalent to AES-128)
  - `ML-KEM-768` (NIST Security Category 3, equivalent to AES-192, default recommended)
  - `ML-KEM-1024` (NIST Security Category 5, equivalent to AES-256)
- **Cryptographic Operations:**
  - `KeyGen() -> (PK, SK)`: Generates public encapsulation key and private decapsulation key.
  - `Encaps(PK) -> (Ciphertext, SharedSecret)`: Encapsulates a shared key under the recipient's public key.
  - `Decaps(SK, Ciphertext) -> SharedSecret`: Decapsulates the shared secret using the private key.
- **Key Wrapping Role:** Used exclusively for asymmetric encapsulation of symmetric file keys, mitigating the transmission and storage overhead of pure lattice-based document encryption.

---

## 13. AES-256 Requirements

- **Cipher Specification:** Advanced Encryption Standard with 256-bit key length (**AES-256**).
- **Mode of Operation:** Authenticated encryption mode (GCM — Galois/Counter Mode) or CBC mode with PKCS7 padding. GCM is preferred for built-in ciphertext integrity authentication.
- **Initialization Vector (IV) / Nonce:** A cryptographically secure random 96-bit (GCM) or 128-bit (CBC) IV must be uniquely generated per encryption operation and stored alongside the ciphertext.
- **Key Lifecycle:** Ephemeral AES symmetric keys are generated per EHR document upload, used to encrypt the payload, immediately encapsulated via ML-KEM, and purged from backend volatile memory.

---

## 14. SSE / HMAC-SHA256 Requirements

- **Scheme:** Searchable Symmetric Encryption based on deterministic keyed-hash message authentication codes (**HMAC-SHA256**).
- **Master Index Key ($K_{\text{SSE}}$):** A 256-bit secret key maintained in secure backend configuration.
- **Tag Generation Function:**
  $$T_w = \text{HMAC-SHA256}(K_{\text{SSE}}, \text{normalize}(w))$$
  where $\text{normalize}(w)$ converts keyword tokens to lowercase and strips leading/trailing whitespace.
- **Trapdoor Generation Function:** The trapdoor function identical to the tag generator, ensuring exact-match lookup capability without index decryption.
- **Database Indexing:** Search tags must be indexed in PostgreSQL with B-tree indices to support sub-second exact match lookups over millions of entries.

---

## 15. SHA-256 Integrity Requirements

- **Standard:** FIPS PUB 180-4 Secure Hash Standard (SHA-256).
- **Digest Size:** 256 bits (32 bytes), represented as a 64-character hexadecimal string or `bytes32` on-chain.
- **Scope of Hash:** Computed strictly over the raw encrypted file payload bytes ($C_{\text{EHR}}$) before database persistence.
- **On-Chain Anchoring:** The hash digest is passed as a parameter to the Solidity smart contract during record creation and permanently stored in the contract state.
- **Retrieval Invariant:** Retrieval fails with a fatal integrity error if $\text{SHA-256}(C_{\text{EHR}}^{\text{retrieved}}) \neq \text{Hash}_{\text{blockchain}}$.

---

## 16. PostgreSQL Storage Requirements

Replacing legacy NoSQL/MongoDB configurations, PostgreSQL serves as the relational, ACID-compliant zero-knowledge storage layer.

### Schema Architecture:

1. **`users` Table:**
   - `id` (UUID / SERIAL, Primary Key)
   - `wallet_address` (VARCHAR(42), Unique, Not Null) — Ethereum wallet address
   - `role` (VARCHAR(20), Not Null) — `PATIENT` or `DOCTOR`
   - `kyber_public_key` (BYTEA / TEXT, Not Null) — Hex/Base64 encoded ML-KEM public key
   - `created_at` (TIMESTAMP WITH TIME ZONE)

2. **`ehr_records` Table:**
   - `id` (UUID, Primary Key)
   - `patient_wallet` (VARCHAR(42), Foreign Key -> users.wallet_address)
   - `uploader_wallet` (VARCHAR(42), Foreign Key -> users.wallet_address)
   - `encrypted_file_payload` (BYTEA / Large Object, Not Null) — AES-256 encrypted EHR content
   - `encapsulated_key_bundle` (BYTEA / TEXT, Not Null) — ML-KEM wrapped AES key
   - `iv_nonce` (BYTEA, Not Null) — AES initialization vector
   - `sha256_file_hash` (VARCHAR(64), Not Null) — Checksum anchored on-chain
   - `blockchain_tx_hash` (VARCHAR(66)) — Ethereum transaction identifier
   - `created_at` (TIMESTAMP WITH TIME ZONE)

3. **`record_search_tags` Table:**
   - `id` (BIGSERIAL, Primary Key)
   - `record_id` (UUID, Foreign Key -> ehr_records.id ON DELETE CASCADE)
   - `search_tag` (VARCHAR(64), Not Null) — HMAC-SHA256 digest
   - *Index:* `CREATE INDEX idx_record_search_tags ON record_search_tags(search_tag);`

---

## 17. Blockchain & Smart Contract Requirements

- **Network:** Ethereum Sepolia Testnet (PoS consensus).
- **Programming Language:** Solidity (`^0.8.20`).
- **Core Smart Contract Responsibilities:**
  1. **Access Control Management:** Maintain an on-chain mapping of authorized doctor addresses per EHR record ID.
  2. **Integrity Hash Registry:** Store the immutable SHA-256 checksum of every uploaded record.
  3. **Event Logging (Audit Trail):** Emit indexed EVM events for every state change and access interaction.

### Smart Contract Interface Specification:
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EHealthShieldACL {
    struct RecordMetadata {
        bytes32 fileHash;
        address ownerPatient;
        uint256 timestamp;
        bool exists;
    }

    // Mapping: recordId => RecordMetadata
    mapping(string => RecordMetadata) private records;

    // Mapping: recordId => (accessorAddress => isAuthorized)
    mapping(string => mapping(address => bool)) private accessPermissions;

    // Events for immutable audit trail
    event RecordUploaded(string indexed recordId, address indexed uploader, address indexed patient, bytes32 fileHash, uint256 timestamp);
    event AccessGranted(string indexed recordId, address indexed patient, address indexed doctor, uint256 timestamp);
    event AccessRevoked(string indexed recordId, address indexed patient, address indexed doctor, uint256 timestamp);
    event RecordSearched(address indexed searcher, uint256 timestamp);
    event RecordRetrieved(string indexed recordId, address indexed accessor, uint256 timestamp);

    function registerRecord(string memory recordId, address patient, bytes32 fileHash) external;
    function grantAccess(string memory recordId, address doctor) external;
    function revokeAccess(string memory recordId, address doctor) external;
    function checkAccess(string memory recordId, address accessor) external view returns (bool);
    function logSearchEvent() external;
    function logRetrievalEvent(string memory recordId) external;
}
```

---

## 18. Access Control Requirements

- **Multi-Factor Cryptographic Authorization:**
  1. **Web3 Identity:** Requester proves ownership of their Ethereum address via EIP-712 typed data signing or personal signature.
  2. **Smart Contract Authorization:** The contract verifies caller address against `accessPermissions[recordId][caller] || records[recordId].ownerPatient == caller`.
  3. **Lattice Decryption Barrier:** Even if network communication is intercepted, data cannot be deciphered without the private ML-KEM key ($SK_{\text{Patient}}$) held client-side.
- **Revocability:** Patients can invoke `revokeAccess` on-chain at any time, instantly invalidating the doctor's retrieval rights for future queries.

---

## 19. Audit Requirements

- **Immutability:** All audit records are written to the Ethereum Sepolia blockchain as emitted transaction logs and permanent storage mappings.
- **Traceability:** Every audit log includes:
  - Actor Ethereum wallet address
  - Target Record ID
  - Event Type (`UPLOAD`, `SEARCH`, `RETRIEVAL`, `GRANT_ACCESS`, `REVOKE_ACCESS`)
  - Timestamp (block timestamp)
  - Integrity Checksum (`bytes32` file hash)
- **Transparency & Patient Oversight:** The React frontend allows patients to query and inspect all historical on-chain events associated with their wallet address, guaranteeing full HIPAA/GDPR audit visibility.

---

## 20. Frontend Requirements

- **UI Framework:** React.js single-page application with modern, responsive CSS.
- **Web3 Connectivity:** MetaMask wallet integration via Ethers.js for signing authentication payloads and triggering smart contract state transitions.
- **Doctor Views:**
  - Document Upload Portal (file selector, patient wallet input, keyword extractor, upload status indicator).
  - Search Console (keyword query input, trapdoor search results table, metadata viewer).
  - Retrieval Interface (one-click record download and integrity verification trigger).
- **Patient Views:**
  - Medical History Dashboard (list of registered EHRs, uploader metadata, upload timestamps).
  - Access Permissions Manager (grant/revoke permissions for specific doctor wallet addresses).
  - Audit Trail Viewer (chronological feed of on-chain access and search events).

---

## 21. Security Guarantees

1. **Post-Quantum Security:** Immune to Shor's algorithm attacks and resistant to Harvest Now, Decrypt Later adversaries via NIST FIPS 203 ML-KEM key encapsulation.
2. **Zero-Knowledge Cloud Isolation:** PostgreSQL storage and backend intermediate layers possess zero knowledge of plaintext medical records, extracted clinical keywords, or user search queries.
3. **Cryptographic Tamper-Proofing:** Any modification of encrypted files in storage is immediately flagged through SHA-256 on-chain hash verification prior to decryption.
4. **Indelible Non-Repudiation:** Storage administrators cannot alter or forge access audit logs without controlling the private keys of authorized actors and breaking the Ethereum consensus mechanism.
5. **Confidentiality:** Bulk clinical data is shielded by industry-standard AES-256 symmetric encryption.

---

## 22. Research Gaps and Claimed Contribution

### Research Gaps Identified in Literature:
- **Gap 1 (Quantum Vulnerability in Blockchain EHR):** Existing systems (e.g., EHRChain, MedShard, HealthChain, BCIF-EHR) rely exclusively on classical RSA/ECC encryption, creating an existential vulnerability to quantum attacks.
- **Gap 2 (Absence of Unified Framework):** Prior academic works treat PQC, SSE, and Blockchain as isolated domains; no deployed framework successfully integrates all three primitives.
- **Gap 3 (Searchability Trade-off):** Fully encrypted EHR storage frameworks lack efficient, blind keyword-searchable mechanisms, causing severe latency and bandwidth overhead.
- **Gap 4 (Centralized Audit Vulnerabilities):** Standard healthcare platforms rely on mutable database audit logs vulnerable to insider tampering.
- **Gap 5 (Absence of Real-World Working Prototypes):** Most PQC healthcare literature remains theoretical or simulation-only without full-stack web integration.

### Claimed Contribution of E-HealthShield:
E-HealthShield is the **first unified full-stack framework** that simultaneously resolves the quantum vulnerability crisis, the encrypted searchability trade-off, and audit integrity by combining:
1. **NIST FIPS 203 CRYSTALS-Kyber (ML-KEM)** for quantum-resistant key encapsulation.
2. **Searchable Symmetric Encryption (SSE with HMAC-SHA256)** for zero-knowledge blind keyword indexing.
3. **Ethereum Smart Contracts (Solidity)** for decentralized, immutable access control and audit logging.
4. **Java Spring Boot + PostgreSQL + React.js** as a practical, production-ready full-stack prototype.

---

## 23. Known Limitations

1. **Prototype Maturity:** Developed as an academic and research prototype; requires formal HIPAA/GDPR clinical compliance certification before production hospital deployment.
2. **Blockchain Latency & Gas Costs:** Public testnet (Ethereum Sepolia) transaction confirmation times and gas fees represent throughput bottlenecks during high-volume hospital operations (mitigable via Layer-2 Rollups or consortium sidechains in future phases).
3. **Search Expressiveness:** Current SSE implementation supports exact-match keyword queries via HMAC-SHA256; complex conjunctive, disjunctive, or fuzzy semantic queries are not yet supported.
4. **Key Management Overhead:** Patients are responsible for securely maintaining their Web3 wallet and Kyber private key credentials; recovery workflows for lost private keys require secondary threshold mechanisms in future releases.