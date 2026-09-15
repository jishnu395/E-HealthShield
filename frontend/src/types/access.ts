export interface GrantAccessRequest {
  recordId: string;
  doctorWallet: string;
}

export interface RevokeAccessRequest {
  recordId: string;
  doctorWallet: string;
}

export interface AccessCheckResponse {
  recordId: string;
  doctorWallet: string;
  hasAccess: boolean;
}

export interface AccessPermissionResponse {
  id: string;
  recordId: string;
  doctorWallet: string;
  patientWallet: string;
  isGranted: boolean;
  blockchainTxHash: string;
  updatedAt: string;
}
