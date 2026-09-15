import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useWallet } from '../context/WalletContext';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { UserRole } from '../types/auth';
import { Shield, Wallet, UserCheck, Stethoscope, Key, CheckCircle, AlertTriangle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { loginWithMetaMask, registerWithMetaMask, isLoading, error, isAuthenticated, role } = useAuth();
  const { account, connectWallet, error: walletError } = useWallet();
  const navigate = useNavigate();

  const [mode, setMode] = useState<'LOGIN' | 'REGISTER'>('LOGIN');
  const [selectedRole, setSelectedRole] = useState<UserRole>('PATIENT');

  React.useEffect(() => {
    if (isAuthenticated && role) {
      navigate(role === 'DOCTOR' ? '/doctor' : '/patient');
    }
  }, [isAuthenticated, role, navigate]);

  const handleLogin = async () => {
    const success = await loginWithMetaMask();
    if (success) {
      // Role-based redirect triggered in useEffect
    }
  };

  const handleRegister = async () => {
    const success = await registerWithMetaMask(selectedRole);
    if (success) {
      // Role-based redirect triggered in useEffect
    }
  };

  return (
    <div style={{
      maxWidth: '520px',
      margin: '3rem auto',
      padding: '2.5rem',
      backgroundColor: '#ffffff',
      borderRadius: '0.75rem',
      boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
      fontFamily: 'system-ui, -apple-system, sans-serif'
    }}>
      {/* Header Badge */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div style={{
          backgroundColor: '#e0f2fe',
          padding: '0.75rem',
          borderRadius: '50%',
          marginBottom: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <Shield size={40} style={{ color: '#0284c7' }} />
        </div>
        <h1 style={{ fontSize: '1.6rem', color: '#0f172a', fontWeight: '700', margin: 0 }}>E-HealthShield Authentication</h1>
        <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: '0.25rem', textAlign: 'center' }}>
          Quantum-Secure Web3 EHR Portal & Post-Quantum Encryption Engine
        </p>
      </div>

      {/* Wallet Status Badge */}
      <div style={{
        backgroundColor: account ? '#f0fdf4' : '#f8fafc',
        border: `1px solid ${account ? '#bbf7d0' : '#e2e8f0'}`,
        borderRadius: '0.5rem',
        padding: '0.75rem 1rem',
        marginBottom: '1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Wallet size={18} style={{ color: account ? '#16a34a' : '#64748b' }} />
          <span style={{ fontSize: '0.85rem', fontWeight: '500', color: account ? '#15803d' : '#475569' }}>
            {account ? `Wallet Connected: ${account.substring(0, 6)}...${account.substring(38)}` : 'MetaMask Disconnected'}
          </span>
        </div>
        {!account && (
          <button
            onClick={connectWallet}
            style={{
              backgroundColor: '#38bdf8',
              color: '#0f172a',
              border: 'none',
              padding: '0.25rem 0.5rem',
              borderRadius: '0.25rem',
              fontSize: '0.75rem',
              fontWeight: '600',
              cursor: 'pointer'
            }}
          >
            Connect
          </button>
        )}
      </div>

      {/* Mode Switcher Tabs */}
      <div style={{
        display: 'flex',
        backgroundColor: '#f1f5f9',
        borderRadius: '0.5rem',
        padding: '0.25rem',
        marginBottom: '1.5rem'
      }}>
        <button
          onClick={() => setMode('LOGIN')}
          style={{
            flex: 1,
            padding: '0.5rem',
            border: 'none',
            borderRadius: '0.375rem',
            fontWeight: '600',
            fontSize: '0.875rem',
            cursor: 'pointer',
            backgroundColor: mode === 'LOGIN' ? '#ffffff' : 'transparent',
            color: mode === 'LOGIN' ? '#0f172a' : '#64748b',
            boxShadow: mode === 'LOGIN' ? '0 1px 2px rgba(0,0,0,0.05)' : 'none'
          }}
        >
          Sign In
        </button>
        <button
          onClick={() => setMode('REGISTER')}
          style={{
            flex: 1,
            padding: '0.5rem',
            border: 'none',
            borderRadius: '0.375rem',
            fontWeight: '600',
            fontSize: '0.875rem',
            cursor: 'pointer',
            backgroundColor: mode === 'REGISTER' ? '#ffffff' : 'transparent',
            color: mode === 'REGISTER' ? '#0f172a' : '#64748b',
            boxShadow: mode === 'REGISTER' ? '0 1px 2px rgba(0,0,0,0.05)' : 'none'
          }}
        >
          Register User
        </button>
      </div>

      {/* Error Displays */}
      {error && <ErrorAlert message={error} />}
      {walletError && <ErrorAlert message={walletError} />}

      {/* SIGN IN FORM */}
      {mode === 'LOGIN' && (
        <div>
          <div style={{
            backgroundColor: '#f8fafc',
            borderRadius: '0.5rem',
            padding: '1rem',
            marginBottom: '1.5rem',
            fontSize: '0.85rem',
            color: '#475569',
            lineHeight: '1.4'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', color: '#0f172a', fontWeight: '600' }}>
              <Key size={16} style={{ color: '#0284c7' }} />
              Zero-Knowledge Web3 Authentication
            </div>
            Signing in requests an EIP-191 challenge nonce from the Spring Boot backend and prompts MetaMask to sign it.
          </div>

          <button
            onClick={handleLogin}
            disabled={isLoading}
            style={{
              width: '100%',
              backgroundColor: '#0284c7',
              color: '#ffffff',
              border: 'none',
              padding: '0.875rem',
              borderRadius: '0.375rem',
              fontSize: '1rem',
              fontWeight: '600',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              boxShadow: '0 2px 4px rgba(2, 132, 199, 0.3)'
            }}
          >
            <UserCheck size={20} />
            {isLoading ? 'Waiting for MetaMask Signature...' : 'Sign In with MetaMask'}
          </button>
        </div>
      )}

      {/* REGISTER FORM */}
      {mode === 'REGISTER' && (
        <div>
          <label style={{ display: 'block', fontWeight: '600', fontSize: '0.875rem', color: '#1e293b', marginBottom: '0.5rem', textAlign: 'left' }}>
            Select User Role:
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div
              onClick={() => setSelectedRole('PATIENT')}
              style={{
                border: `2px solid ${selectedRole === 'PATIENT' ? '#0284c7' : '#e2e8f0'}`,
                backgroundColor: selectedRole === 'PATIENT' ? '#f0f9ff' : '#ffffff',
                padding: '1rem',
                borderRadius: '0.5rem',
                cursor: 'pointer',
                textAlign: 'center'
              }}
            >
              <UserCheck size={24} style={{ color: selectedRole === 'PATIENT' ? '#0284c7' : '#64748b', marginBottom: '0.25rem' }} />
              <div style={{ fontWeight: '600', fontSize: '0.9rem', color: '#0f172a' }}>PATIENT</div>
              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Data Owner & Decryption</div>
            </div>

            <div
              onClick={() => setSelectedRole('DOCTOR')}
              style={{
                border: `2px solid ${selectedRole === 'DOCTOR' ? '#0284c7' : '#e2e8f0'}`,
                backgroundColor: selectedRole === 'DOCTOR' ? '#f0f9ff' : '#ffffff',
                padding: '1rem',
                borderRadius: '0.5rem',
                cursor: 'pointer',
                textAlign: 'center'
              }}
            >
              <Stethoscope size={24} style={{ color: selectedRole === 'DOCTOR' ? '#0284c7' : '#64748b', marginBottom: '0.25rem' }} />
              <div style={{ fontWeight: '600', fontSize: '0.9rem', color: '#0f172a' }}>DOCTOR</div>
              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Healthcare Provider</div>
            </div>
          </div>

          <div style={{
            backgroundColor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            borderRadius: '0.5rem',
            padding: '0.75rem',
            marginBottom: '1.5rem',
            fontSize: '0.8rem',
            color: '#166534',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.5rem'
          }}>
            <CheckCircle size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
            <span>
              Registration will generate a NIST FIPS 203 ML-KEM-768 keypair. Your 2,400-byte private key is saved <strong>STRICTLY in your browser</strong> and is never sent to the server.
            </span>
          </div>

          <button
            onClick={handleRegister}
            disabled={isLoading}
            style={{
              width: '100%',
              backgroundColor: '#16a34a',
              color: '#ffffff',
              border: 'none',
              padding: '0.875rem',
              borderRadius: '0.375rem',
              fontSize: '1rem',
              fontWeight: '600',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              boxShadow: '0 2px 4px rgba(22, 163, 74, 0.3)'
            }}
          >
            <Shield size={20} />
            {isLoading ? 'Generating Keys & Registering...' : `Register as ${selectedRole}`}
          </button>
        </div>
      )}
    </div>
  );
};
