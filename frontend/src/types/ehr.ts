export interface EhrUploadResponse {
  recordId: string;
  fileName: string;
  contentType: string;
  patientWallet: string;
  fileHash: string;
  createdAt: string;
}

export interface EhrSearchRequest {
  keyword: string;
}

export interface EhrSearchResultResponse {
  recordId: string;
  fileName: string;
  contentType: string;
  patientWallet: string;
  uploaderWallet: string;
  fileHash: string;
  createdAt: string;
}

export interface EhrRetrievalResponse {
  recordId: string;
  fileName: string;
  contentType: string;
  patientWallet: string;
  uploaderWallet: string;
  encryptedCiphertext: string; // Base64 encoded
  iv: string;                  // Base64 encoded
  kemCiphertext: string;       // Base64 encoded
  fileHash: string;
  createdAt: string;
}
