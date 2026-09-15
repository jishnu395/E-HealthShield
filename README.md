# E-HealthShield

[![Backend Tests](https://img.shields.io/badge/Backend%20Tests-118%2F118%20Passed-success.svg)](#testing--verification)
[![Frontend Tests](https://img.shields.io/badge/Frontend%20Tests-18%2F18%20Passed-success.svg)](#testing--verification)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![Solidity](https://img.shields.io/badge/Solidity-%5E0.8.20-lightgrey.svg)](https://soliditylang.org/)
[![Post-Quantum Cryptography](https://img.shields.io/badge/PQC-ML--KEM--768-purple.svg)](https://csrc.nist.gov/pubs/fips/203/final)

**E-HealthShield** is a completed, post-quantum cryptography-enabled, and blockchain-auditable Electronic Health Record (EHR) platform. Designed to address security vulnerabilities of legacy medical databases—such as single-point-of-failure data breaches, unauthorized access, harvest-now-decrypt-later (HNDL) quantum threats, and opaque access logs—E-HealthShield combines NIST FIPS 203 Post-Quantum Cryptography (ML-KEM-768), client-side AES-256-GCM authenticated encryption, deterministic HMAC-SHA256 blind keyword search, and Ethereum Smart Contract Access Control Lists (ACL). The system features a modular, security-oriented architecture powered by a Java / Spring Boot backend, a React 18 + Vite + TypeScript frontend, PostgreSQL storage, and an Ethereum EVM smart contract deployment.

---

## Overview

### The Problem
Electronic Health Records contain highly sensitive Patient Health Information (PHI). Traditional centralized EHR architectures suffer from structural weaknesses:
1. **Centralized Data Exposure**: Storage of unencrypted or server-encrypted health records leaves databases vulnerable to internal rogue administrators and external data breaches.
2. **Quantum Decryption Threats**: Advances in quantum computing expose standard asymmetric cryptography (RSA, ECC) to retrospective decryption ("Harvest-Now, Decrypt-Later" attacks).
3. **Opaque Access Control & Audit Logs**: Centralized access logs can be altered, truncated, or fabricated by database administrators, preventing patients from verifying access history.
4. **Keyword Exposure during Search**: Traditional database search operations require plaintext indexes or server-side decryption of medical search terms.

### Purpose of E-HealthShield
E-HealthShield addresses these security challenges through a client-side encrypted architecture:
- **Post-Quantum Key Encapsulation**: Enforces ML-KEM-768 (Crystals-Kyber) key encapsulation so that record encryption keys remain protected against quantum adversaries.
- **Client-Side Cryptography**: Encrypts EHR payloads directly inside the patient's or doctor's web browser using AES-256-GCM prior to transmission. The patient's ML-KEM private key is stored in browser local storage and is **never transmitted to the backend server**.
- **Searchable Encrypted Data**: Enables doctors to perform keyword searches using deterministic HMAC-SHA256 search trapdoors without storing plaintext keywords in the searchable index.
- **Blockchain Access Control & Auditing**: Enforces patient-managed access permissions on an Ethereum EVM smart contract ([EHealthShieldACL.sol](blockchain/contracts/EHealthShieldACL.sol)). Record upload, access grant, revocation, search, and retrieval events are anchored on-chain as a tamper-evident audit trail.

This software implementation is **fully completed**, verified with automated test suites, and ready for evaluation and local deployment.

---

## Key Features

- **MetaMask Wallet & EIP-191 Authentication**: Passwordless identity verification using Web3 Ethereum wallet addresses and EIP-191 cryptographic challenge-response signature verification.
- **Stateless JWT Session Management**: Role-scoped JSON Web Tokens (JWT) for authenticated REST API interaction.
- **Patient & Doctor Role Governance**: Enforces boundary isolation between Patient and Doctor workflows across frontend UI routes and backend API endpoints.
- **Client-Side AES-256-GCM Payload Encryption**: EHR payloads (clinical reports, lab diagnostics) are encrypted on the client using 256-bit AES-GCM with unique initialization vectors (IV) and authentication tags.
- **ML-KEM-768 Key Encapsulation (NIST FIPS 203)**: Dual-layer key protection where the AES key is encapsulated under the recipient's ML-KEM-768 public key.
- **Client-Side Decapsulation & Decryption**: Records are decapsulated and decrypted locally inside the recipient browser using their private key stored in browser local storage.
- **HMAC-SHA256 Blind Keyword Search**: Deterministic search tag generation enables database querying without storing plaintext keywords in the searchable index.
- **Smart Contract Access Control (ACL)**: Patient-controlled doctor access grants and revocations managed directly via smart contract logic.
- **SHA-256 Integrity Anchoring**: Uploaded records register their SHA-256 ciphertext digest on the blockchain to detect payload tampering or data corruption.
- **Tamper-Evident On-Chain Audit Logging**: Emission of Solidity smart contract events (`RecordUploaded`, `AccessGranted`, `AccessRevoked`, `RecordSearched`, `RecordRetrieved`).
- **Zero Private-Key Transmission**: Patient ML-KEM private decapsulation keys remain strictly isolated within browser local storage.

---

## Security Architecture

### Technical Encryption & Retrieval Workflow

```
[ Doctor / Patient ]
         │
         ├── 1. MetaMask EIP-191 Challenge-Response Sign ──► Backend Auth ──► JWT Issued
         │
         ├── 2. Client-Side AES-256-GCM Payload Encryption
         │      ├── Encrypts Plaintext File -> Ciphertext + IV + Auth Tag
         │      └── Computes SHA-256 Digest of Ciphertext
         │
         ├── 3. Post-Quantum Key Encapsulation (ML-KEM-768)
         │      └── Encapsulate(Patient_PK) -> (KEM Ciphertext, 32-byte AES Shared Secret)
         │
         ├── 4. Blind Keyword Tag Generation
         │      └── HMAC-SHA256(SSE_Master_Key, Keyword) -> Blind Search Tag
         │
         ├── 5. REST Upload to Backend & Smart Contract Registration
         │      ├── Payload stored in PostgreSQL Database
         │      └── On-Chain Anchor: registerRecord(recordId, patient, sha256Digest)
         │
         └── 6. Retrieval, Decapsulation & Decryption
                ├── Backend validates JWT + Smart Contract checkAccess(recordId, accessor)
                ├── Backend logs on-chain: logRetrievalEvent(recordId)
                ├── Client receives Encrypted Bundle
                ├── Client Decapsulates: Decaps(KEM Ciphertext, Patient_SK) -> AES Shared Secret
                └── Client Decrypts: AES-256-GCM Decrypt(Ciphertext, AES Shared Secret) -> Plaintext EHR
```

### Role of Cryptographic & Security Mechanisms

| Primitive / Mechanism | Implementation | Security Purpose |
| :--- | :--- | :--- |
| **AES-256-GCM** | Client-Side / Web Crypto | Authenticated symmetric encryption ensuring EHR payload confidentiality and data integrity. |
| **ML-KEM-768** | `@noble/post-quantum` & Bouncy Castle | NIST FIPS 203 Post-Quantum Key Encapsulation Mechanism protecting symmetric keys against quantum attacks. |
| **SHA-256** | Client & Backend Hash | Generates payload fingerprints anchored to the blockchain for tamper verification. |
| **HMAC-SHA256 Search**| `SseService` | Deterministic search tag/trapdoor generation for searching encrypted records without storing plaintext keywords in the index. |
| **EIP-191 & Web3** | MetaMask / `Web3SignatureVerifier` | Cryptographic authentication deriving user identities directly from Ethereum wallet signatures. |
| **JWT (JSON Web Token)**| `JwtService` | Stateless bearer token authentication for REST API endpoints. |
| **Smart Contract ACL** | `EHealthShieldACL.sol` | Decentralized, patient-owned access control logic enforcing grant/revoke rules on the EVM. |
| **On-Chain Audit Trail**| Solidity Event Logs | Tamper-evident record of record uploads, access modifications, searches, and retrievals. |

---

## System Architecture

E-HealthShield is architected into three primary decoupled tiers:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             FRONTEND TIER                                   │
│    React 18 + TypeScript + Vite + Lucide Icons + Ethers.js + `@noble/pqc`   │
│   (MetaMask Authentication, Client-Side Encryption/Decapsulation, Workspaces) │
└──────────────────────┬──────────────────────────────┬───────────────────────┘
                       │ REST API (JWT)               │ JSON-RPC (Web3)
                       ▼                              ▼
┌─────────────────────────────────────────┐  ┌────────────────────────────────┐
│              BACKEND TIER                │  │        BLOCKCHAIN TIER         │
│         Java 17 / Spring Boot 4.1       │  │   Hardhat EVM Local Network    │
│  (REST Controllers, Spring Security,    │  │     EHealthShieldACL.sol       │
│   Crypto Services, EthJsonRpcClient)    │  │  (Record Anchoring, ACL, Audit)│
└──────────────────────┬──────────────────┘  └────────────────────────────────┘
                       │ JPA / Hibernate
                       ▼
┌─────────────────────────────────────────┐
│              DATABASE TIER              │
│               PostgreSQL                │
│  (Users, Encrypted EHRs, Search Tags)   │
└─────────────────────────────────────────┘
```

### 1. Backend (`backend/`)
Built with Java 17+ and Spring Boot 4.1.0, the backend manages REST API requests, user profile persistence, encrypted record storage, search trapdoor evaluation, and Ethereum blockchain RPC interaction (`EthJsonRpcClient`).

### 2. Frontend (`frontend/`)
Built with React 18.3, Vite, and TypeScript, the frontend provides dedicated Patient and Doctor portals. It executes client-side ML-KEM-768 keypair generation, client-side AES-256-GCM encryption/decryption, and MetaMask wallet interactions. Private keys are maintained in browser local storage.

### 3. Blockchain (`blockchain/`)
Powered by Hardhat and Solidity `^0.8.20`, the `EHealthShieldACL` contract manages on-chain record anchoring, access permission matrices, and audit event logging.

---

## Technology Stack

| Layer | Component / Tool | Version / Details |
| :--- | :--- | :--- |
| **Backend** | Java | 17+ (Java 22 tested) |
| | Spring Boot | 4.1.0 |
| | Security | Spring Security + JWT (io.jsonwebtoken) |
| | Database | PostgreSQL 14+ |
| | Cryptography | Bouncy Castle (`bcprov-jdk18on` v1.80) |
| | Build Tool | Maven 3.x (`mvnw`) |
| **Frontend** | Framework | React 18.3 |
| | Language | TypeScript 5.5 |
| | Build Tool | Vite 5.4 |
| | Web3 / EVM | Ethers.js v6.13 |
| | Client PQC | `@noble/post-quantum` v0.7.0 |
| | Testing | Vitest v2.0 + Testing Library |
| **Blockchain** | Smart Contract | Solidity `^0.8.20` |
| | Environment | Hardhat v2.22 |
| | Network | Hardhat Local EVM (Chain ID 31337) / Sepolia Testnet |
| **Cryptography** | PQC Key Encapsulation | ML-KEM-768 (NIST FIPS 203) |
| | Payload Encryption | AES-256-GCM (Authenticated Encryption) |
| | Search Indexing | Blind HMAC-SHA256 Keyword Tagging |
| | Integrity Hash | SHA-256 |

---

## Project Structure

```
E-HealthShield/
├── backend/                        # Java / Spring Boot backend application
│   ├── src/
│   │   ├── main/java/com/ehealthshield/backend/
│   │   │   ├── blockchain/        # EthJsonRpcClient, EthereumBlockchainService, SolidityAbiEncoder
│   │   │   ├── config/            # AppConfig, CorsConfig, SecurityConfig, DevDataInitializer
│   │   │   ├── controller/        # AuthController, EhrController, AccessController, AuditController
│   │   │   ├── crypto/            # AesCryptoService, MlKemCryptoService, SseService, HashService
│   │   │   ├── dto/               # Request & Response DTO objects
│   │   │   ├── entity/            # UserEntity, EhrRecordEntity, AccessPermissionEntity
│   │   │   ├── repository/        # JPA Repositories
│   │   │   ├── security/          # JwtService, Web3SignatureVerifier, ChallengeService
│   │   │   └── service/           # EhrService, AclService, SearchService, RetrievalService, AuditService
│   │   └── test/java/...          # 24 Backend Automated Test Classes (118 Tests)
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── blockchain/                     # Hardhat EVM smart contract environment
│   ├── contracts/
│   │   └── EHealthShieldACL.sol    # Smart contract for EHR registration, ACL, and Audit Events
│   ├── scripts/
│   │   └── deploy.js               # Contract deployment script
│   ├── hardhat.config.js           # Hardhat network & compiler configuration
│   └── package.json
├── frontend/                       # React + Vite + TypeScript web application
│   ├── src/
│   │   ├── api/                    # Axios API client & interceptors
│   │   ├── components/             # Common UI components, Navbar, Route guards
│   │   ├── context/                # AuthContext, WalletContext (MetaMask)
│   │   ├── pages/                  # Doctor & Patient dashboards, upload, search, access, audit pages
│   │   ├── utils/                  # cryptoUtils.ts (Noble ML-KEM-768 & AES-GCM interop)
│   │   └── __tests__/              # Vitest test suites (18 Tests)
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
├── crypto/                         # Standalone Python cryptographic reference implementation
│   ├── aes_handler.py, kyber_handler.py, sse_handler.py, test_crypto.py
├── project-docs/                   # Specifications and technical report
│   ├── PROJECT_SPEC.md
│   └── E-HealthShield W final6.pdf
├── sample_ehr_report.txt / sample_ehr_phase*.txt # Test EHR payload files
├── test_upload.ps1                 # PowerShell API upload test script
├── implementation_plan.md          # Architectural plan & technical specifications
├── .gitignore                      # Root repository gitignore rules
└── README.md
```

---

## Core Workflows

### 1. Authentication Workflow
1. User connects their MetaMask Web3 wallet to the frontend application.
2. The frontend requests a cryptographic nonce challenge from `/api/auth/challenge?walletAddress=0x...`.
3. User signs an EIP-191 formatted challenge message using MetaMask.
4. The frontend posts the signature to `/api/auth/verify`.
5. The backend (`Web3SignatureVerifier`) recovers the signer address from the signature. Upon match, it issues a signed JWT bearer token.

### 2. EHR Upload & Ingestion Workflow
1. A Doctor selects a patient and an EHR payload file (`.txt`, `.pdf`, `.json`).
2. The frontend generates a random 256-bit AES key and encrypts the file using AES-256-GCM.
3. The frontend retrieves the target Patient's ML-KEM-768 public key and encapsulates the AES key using ML-KEM-768 (`@noble/post-quantum`).
4. The frontend computes the SHA-256 hash of the ciphertext payload.
5. The frontend sends the encrypted bundle, ML-KEM ciphertext, SHA-256 hash, and search keywords to the backend.
6. The backend computes HMAC-SHA256 search tags (`SseService`), stores the record in PostgreSQL, and submits a transaction to the smart contract via `registerRecord(recordId, patientAddress, fileHash)`.

### 3. Blind Keyword Search Workflow
1. A Doctor enters a medical keyword search query (e.g., `"cardiology"`).
2. The backend computes a deterministic search trapdoor `HMAC-SHA256(sseMasterKey, keyword)`.
3. The backend queries the database index for matching record search tags without storing plaintext keywords in the searchable index.
4. The backend invokes `blockchainService.logSearchEvent(searcherWallet)` to record the search event on the Ethereum smart contract audit trail.
5. Matching record metadata is returned to the doctor.

### 4. EHR Retrieval and Decryption Workflow
1. An authorized user (Patient owner or authorized Doctor) requests record retrieval.
2. The backend queries the smart contract `checkAccess(recordId, accessorAddress)` to verify access rights on-chain.
3. Upon positive verification, the backend emits `logRetrievalEvent(recordId)` on the blockchain and returns the encrypted record bundle.
4. The recipient's browser receives the bundle, decapsulates the AES key using their private key stored in browser local storage (`decapsulateAesKey`), and decrypts the AES-256-GCM ciphertext to recover the original EHR payload.

### 5. Patient Access Control Governance
1. Patients view all registered records on their Patient Access Management dashboard.
2. Patients can grant or revoke access for specific Doctor wallet addresses.
3. Access modifications invoke smart contract functions `grantAccess(recordId, doctorAddress)` or `revokeAccess(recordId, doctorAddress)`, updating on-chain permissions and emitting audit events.

### 6. Audit & Integrity Verification Workflow
1. The Patient or Doctor navigates to the Audit Trail dashboard.
2. The application queries on-chain events (`RecordUploaded`, `AccessGranted`, `AccessRevoked`, `RecordSearched`, `RecordRetrieved`) from the blockchain contract.
3. The application compares the current database payload's SHA-256 digest against the on-chain anchored digest `getRecord(recordId).fileHash`. Any discrepancy flags a visual tamper alert.

---

## Smart Contract

The core access control and audit contract is [EHealthShieldACL.sol](blockchain/contracts/EHealthShieldACL.sol).

### Key Contract Structures & Functions

```solidity
struct RecordMetadata {
    bytes32 fileHash;         // SHA-256 hash anchor of the encrypted payload
    address ownerPatient;     // Wallet address of the patient record owner
    address uploaderDoctor;   // Wallet address of the uploading doctor
    uint256 timestamp;        // Block timestamp of record registration
    bool exists;              // Existence flag
}

function registerRecord(string memory recordId, address patient, bytes32 fileHash) external;
function grantAccess(string memory recordId, address doctor) external;
function revokeAccess(string memory recordId, address doctor) external;
function checkAccess(string memory recordId, address accessor) external view returns (bool);
function logSearchEvent() external;
function logRetrievalEvent(string memory recordId) external;
function getRecord(string memory recordId) external view returns (...);
```

---

## Installation and Setup

### Prerequisites
- **Java Development Kit (JDK)**: JDK 17 or higher
- **Node.js**: v18.0.0 or higher (with `npm`)
- **Database**: PostgreSQL 14+ running locally on port `5432`
- **Browser Extension**: MetaMask installed in Chrome/Brave/Firefox

### Environment Configuration

1. **Backend Configuration**:
   Create or set environment variables for the backend:
   ```env
   DB_USERNAME=postgres
   DB_PASSWORD=your_postgres_password
   APP_CRYPTO_SSE_MASTER_KEY=your_base64_32byte_master_key
   ETH_RPC_URL=http://127.0.0.1:8545
   CONTRACT_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
   CHAIN_ID=31337
   SIGNER_ADDRESS=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
   ```

2. **Frontend Configuration**:
   Copy `frontend/.env.example` to `frontend/.env`:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   VITE_CHAIN_ID=31337
   VITE_CONTRACT_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
   ```

---

## Running the Project

Follow these steps to launch the E-HealthShield system locally:

### Step 1: Start PostgreSQL
Ensure PostgreSQL is running and create the target database:
```sql
CREATE DATABASE ehealthshield_db;
```

### Step 2: Start Hardhat Local Blockchain
Open a terminal in `blockchain/` and start the local EVM node:
```bash
cd blockchain
npm install
npx hardhat node
```
*Note: Hardhat node outputs 20 development accounts with test ETH and private keys for local testing.*

### Step 3: Deploy Smart Contract
In a second terminal, deploy the `EHealthShieldACL` contract to the local node:
```bash
cd blockchain
npm run deploy:local
```
*Take note of the deployed contract address (default: `0x5FbDB2315678afecb367f032d93F642f64180aa3`).*

### Step 4: Start Spring Boot Backend
In a third terminal, start the Java backend service:
```bash
cd backend
./mvnw spring-boot:run
```
The backend server will start on `http://localhost:8080`.

### Step 5: Start React Frontend
In a fourth terminal, launch the frontend development server:
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

---

## Configuration Reference

| Environment Variable | Target Component | Description | Default / Example Value |
| :--- | :--- | :--- | :--- |
| `DB_USERNAME` | Backend | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | Backend | PostgreSQL password | `root` |
| `APP_CRYPTO_SSE_MASTER_KEY` | Backend | Base64 32-byte master key for SSE HMAC tag generation | *(Set in environment)* |
| `ETH_RPC_URL` | Backend | EVM RPC endpoint | `http://127.0.0.1:8545` |
| `CONTRACT_ADDRESS` | Backend / Frontend | Deployed `EHealthShieldACL` address | `0x5FbDB2315678afecb367f032d93F642f64180aa3` |
| `CHAIN_ID` | Backend / Frontend | EVM Network Chain ID | `31337` (Hardhat Local) |
| `SIGNER_ADDRESS` | Backend | Relayer/Admin EVM signer address | `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266` |
| `VITE_API_BASE_URL` | Frontend | Backend API base URL | `http://localhost:8080` |
| `SEPOLIA_RPC_URL` | Blockchain | Sepolia Testnet RPC URL (optional) | `https://rpc.sepolia.org` |
| `SEPOLIA_PRIVATE_KEY` | Blockchain | Sepolia deployer private key (optional) | *(Set in environment)* |

---

## Testing & Verification

The repository includes automated unit and integration tests across the backend, frontend, and smart contract components:

### Automated Test Results
- **Backend Test Suite**: **118 / 118 PASSED** (`./mvnw test`)
  - 24 test classes covering AES-256-GCM encryption/decryption, ML-KEM-768 key encapsulation, Noble JS/Java interop, HMAC-SHA256 SSE search, JWT token lifecycle, EIP-191 signature verification, REST controllers, and ACL services.
- **Frontend Test Suite**: **18 / 18 PASSED** (`npm test`)
  - 4 test files covering client-side ML-KEM key generation, decapsulation, AES decryption, MetaMask auth state, patient, and doctor workspace routes.
- **Frontend Production Build**: **PASSED** (`npm run build`)
- **Smart Contract Compilation**: **PASSED** (`npx hardhat compile`)

### Tested Security Scenarios
- **Crypto Interoperability**: Verified byte-for-byte encapsulation and decapsulation compatibility between `@noble/post-quantum` (JS) and BouncyCastle (Java).
- **Tampered Ciphertext Rejection**: AES-256-GCM authentication tag validation rejects modified ciphertexts.
- **Unauthorized Access Rejection**: Access requests for revoked doctors trigger smart-contract access rejection.
- **Private Key Non-Transmission**: Verified that ML-KEM decapsulation private keys remain in browser local storage and are never included in HTTP request payloads.

---

## Security Considerations

1. **Client-Side Key Isolation**: The patient's ML-KEM-768 decapsulation private key is generated and stored locally in browser local storage. It is never logged or transmitted over the network.
2. **Stateless JWT Security**: JWT tokens are signed using a secret key, scoped with user role claims, and enforced on protected REST routes via `JwtAuthenticationFilter`.
3. **Decentralized ACL Enforcement**: Access rights are evaluated directly against the smart contract ledger, preventing backend database manipulation from overriding patient access revokes.
4. **Development vs. Production Hardening**:
   - Local Hardhat accounts and private keys must be replaced with secure hardware wallets or managed KMS signers in production.
   - Database credentials and master SSE keys should be injected using secret managers (e.g., AWS Secrets Manager, HashiCorp Vault).

---

## Research & Technical Scope

E-HealthShield demonstrates a practical synthesis of cryptographic primitives and decentralized technology for health data management:
- **Post-Quantum Resilience**: Adopts NIST FIPS 203 (ML-KEM-768) to protect symmetric encryption key exchanges against quantum decryption attacks.
- **Data Privacy & Searchability**: Utilizes HMAC-SHA256 blind keyword indexing to enable database search without storing plaintext keywords in the index.
- **Blockchain Governance**: Replaces server-managed ACLs with Ethereum smart contract governance and tamper-evident event logging.

---

## Completed Implementation

The E-HealthShield project is **fully completed**. All core components—including Web3 authentication, ML-KEM-768 key encapsulation, client-side AES-256-GCM payload encryption, blind keyword search, Solidity smart contract ACL governance, audit logging, and responsive frontend UI—are implemented, integrated, with all recorded automated tests passing.

---

## Limitations

- **Local EVM Scope**: The primary development environment runs on a local Hardhat EVM node (`Chain ID 31337`). Deployment to public testnets (e.g., Sepolia) or mainnet requires active gas management and RPC provider configuration.
- **Deterministic HMAC Search Characteristics**: Searchable encryption relies on deterministic HMAC-SHA256 keyword tags. While plaintext keywords are not stored in the index, deterministic search schemes inherently exhibit equality and frequency leakage across identical keywords.
- **Key Recovery**: Client-side key isolation means clearing browser local storage requires key re-registration unless external backup mechanisms are configured.

---

## Future Enhancements

- **Production Testnet/Mainnet Deployment**: Deployment and gas optimization on Sepolia or Ethereum L2 networks (e.g., Arbitrum, Polygon).
- **Decentralized Storage Integration**: Extending encrypted payload storage from database blobs to IPFS / Arweave.
- **Multi-Party Computation (MPC) Key Recovery**: Implementation of threshold social recovery for patient ML-KEM decapsulation keys.

---

## Documentation References

- [PROJECT_SPEC.md](PROJECT_SPEC.md): Authoritative Technical Specification Document
- [implementation_plan.md](implementation_plan.md): Implementation & Architectural Specifications
- [project-docs/](project-docs/): Academic Phase-1 Report PDF and Design Specs
- [blockchain/contracts/EHealthShieldACL.sol](blockchain/contracts/EHealthShieldACL.sol): Solidity Smart Contract Implementation

---

## Project Information

- **Project Title**: E-HealthShield: A Quantum-Secure and Blockchain-Auditable Framework for Electronic Health Records
- **Institutional Context**: Academic Research Project (Department of Information Science & Engineering / Dept. of ICBT, SMVIT / VTU, 2025–2026)
- **Status**: Completed Implementation

---

## Project Contributors

E-HealthShield was developed as a four-member academic research project by:
- **Jishnu V**
- **Syed Abdulla**
- **Sumanth L**
- **Nischay B**
