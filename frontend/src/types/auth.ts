export type UserRole = 'PATIENT' | 'DOCTOR';

export interface AuthChallengeRequest {
  walletAddress: string;
}

export interface AuthChallengeResponse {
  walletAddress: string;
  challenge: string;
}

export interface AuthVerifyRequest {
  walletAddress: string;
  signature: string;
}

export interface AuthResponse {
  token: string;
  walletAddress: string;
  role: UserRole;
  expiresIn: number;
}

export interface UserRegisterRequest {
  walletAddress: string;
  role: UserRole;
  kyberPublicKeyBase64: string;
}

export interface UserEntity {
  id: string;
  walletAddress: string;
  role: UserRole;
  kyberPublicKeyBase64: string;
  createdAt: string;
}
