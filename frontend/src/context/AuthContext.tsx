import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { UserRole, UserEntity, AuthChallengeResponse, AuthResponse } from '../types/auth';
import { apiClient } from '../api/client';
import { CONFIG } from '../config';
import { useWallet } from './WalletContext';
import { ml_kem768 } from '@noble/post-quantum/ml-kem.js';

interface AuthContextType {
  token: string | null;
  user: UserEntity | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  loginWithMetaMask: () => Promise<boolean>;
  registerWithMetaMask: (selectedRole: UserRole) => Promise<boolean>;
  logout: () => void;
  getPatientPrivateKey: () => Uint8Array | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function bufferToBase64(buffer: Uint8Array): string {
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window.btoa(binary);
}

function base64ToBuffer(base64: string): Uint8Array {
  const binaryString = window.atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem(CONFIG.JWT_STORAGE_KEY));
  const [user, setUser] = useState<UserEntity | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const { account, connectWallet, signMessage } = useWallet();

  // Handle wallet account change away from authenticated wallet
  useEffect(() => {
    if (account && user?.walletAddress && account.toLowerCase() !== user.walletAddress.toLowerCase()) {
      console.warn(`Wallet account changed from ${user.walletAddress} to ${account}. Invalidating session.`);
      logout();
    }
  }, [account, user]);

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem(CONFIG.JWT_STORAGE_KEY);
      if (storedToken) {
        try {
          const res = await apiClient.get<UserEntity>('/api/auth/me', {
            headers: { Authorization: `Bearer ${storedToken}` }
          });
          setUser(res.data);
          setRole(res.data.role);
          setToken(storedToken);
          ensureLocalKyberKeyPair(res.data.walletAddress);
        } catch (err) {
          console.warn('Failed to restore Web3 session from token:', err);
          logout();
        }
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const ensureLocalKyberKeyPair = (walletAddress: string): Uint8Array => {
    const storageKey = `ehealth_kyber_privkey_${walletAddress.toLowerCase()}`;
    const existingPrivKeyBase64 = localStorage.getItem(storageKey);

    if (existingPrivKeyBase64) {
      return base64ToBuffer(existingPrivKeyBase64);
    }

    // Generate fresh NIST FIPS 203 ML-KEM-768 keypair locally using @noble/post-quantum
    const keys = ml_kem768.keygen();
    const privKeyBase64 = bufferToBase64(keys.secretKey);
    localStorage.setItem(storageKey, privKeyBase64);
    return keys.secretKey;
  };

  const getPatientPrivateKey = (): Uint8Array | null => {
    if (!user?.walletAddress) return null;
    const storageKey = `ehealth_kyber_privkey_${user.walletAddress.toLowerCase()}`;
    const base64Key = localStorage.getItem(storageKey);
    return base64Key ? base64ToBuffer(base64Key) : null;
  };

  const loginWithMetaMask = async (): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      // 1. Connect Wallet
      const walletAddress = await connectWallet();
      if (!walletAddress) {
        throw new Error('MetaMask connection rejected or unavailable.');
      }

      // 2. Request Auth Challenge Nonce
      const challengeRes = await apiClient.post<AuthChallengeResponse>('/api/auth/challenge', {
        walletAddress
      });
      const challengeText = challengeRes.data.challenge;

      // 3. Prompt MetaMask personal_sign
      const signature = await signMessage(challengeText);

      // 4. Verify Signature & Receive JWT
      const verifyRes = await apiClient.post<AuthResponse>('/api/auth/verify', {
        walletAddress,
        signature
      });

      const newToken = verifyRes.data.token;
      const userRole = verifyRes.data.role;

      localStorage.setItem(CONFIG.JWT_STORAGE_KEY, newToken);
      setToken(newToken);
      setRole(userRole);

      // 5. Fetch User Profile
      const meRes = await apiClient.get<UserEntity>('/api/auth/me', {
        headers: { Authorization: `Bearer ${newToken}` }
      });

      setUser(meRes.data);
      ensureLocalKyberKeyPair(meRes.data.walletAddress);

      setIsLoading(false);
      return true;
    } catch (err: any) {
      let msg = 'Authentication failed';
      if (err.code === 4001 || err.message?.includes('user rejected')) {
        msg = 'MetaMask signature rejected by user.';
      } else if (err.response?.data?.message) {
        msg = err.response.data.message;
      } else if (err.message) {
        msg = err.message;
      }

      setError(msg);
      setIsLoading(false);
      return false;
    }
  };

  const registerWithMetaMask = async (selectedRole: UserRole): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      const walletAddress = await connectWallet();
      if (!walletAddress) {
        throw new Error('MetaMask connection rejected or unavailable.');
      }

      // Generate local ML-KEM-768 keypair
      const keys = ml_kem768.keygen();
      const pubKeyBase64 = bufferToBase64(keys.publicKey);
      const privKeyBase64 = bufferToBase64(keys.secretKey);

      // Save private key locally in browser localStorage BEFORE registration
      const storageKey = `ehealth_kyber_privkey_${walletAddress.toLowerCase()}`;
      localStorage.setItem(storageKey, privKeyBase64);

      // Register with Spring Boot backend (sending ONLY public key)
      const regRes = await apiClient.post<AuthResponse>('/api/auth/register', {
        walletAddress,
        role: selectedRole,
        kyberPublicKeyBase64: pubKeyBase64
      });

      const newToken = regRes.data.token;
      const userRole = regRes.data.role;

      localStorage.setItem(CONFIG.JWT_STORAGE_KEY, newToken);
      setToken(newToken);
      setRole(userRole);

      const meRes = await apiClient.get<UserEntity>('/api/auth/me', {
        headers: { Authorization: `Bearer ${newToken}` }
      });

      setUser(meRes.data);
      setIsLoading(false);
      return true;
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Registration failed';
      setError(msg);
      setIsLoading(false);
      return false;
    }
  };

  const logout = () => {
    localStorage.removeItem(CONFIG.JWT_STORAGE_KEY);
    setToken(null);
    setUser(null);
    setRole(null);
    setError(null);
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        role,
        isAuthenticated: !!token && !!user,
        isLoading,
        error,
        loginWithMetaMask,
        registerWithMetaMask,
        logout,
        getPatientPrivateKey
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
