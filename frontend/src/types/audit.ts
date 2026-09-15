export interface AuditVerificationResponse {
  recordId: string;
  localCiphertextHash: string;
  onChainAnchoredHash: string;
  blockchainTransactionHash: string;
  isTamperFree: boolean;
  onChainTimestamp: number;
}

export interface OnChainRecordMetadata {
  fileHash: string;
  ownerPatient: string;
  uploaderDoctor: string;
  timestamp: number;
  exists: boolean;
}
