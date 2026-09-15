import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { WalletProvider } from '../context/WalletContext';
import { LoginPage } from '../pages/LoginPage';

const TestComponent = () => {
  const { isAuthenticated, token, user, role, logout } = useAuth();
  return (
    <div>
      <span data-testid="auth-status">{isAuthenticated ? 'AUTHENTICATED' : 'ANONYMOUS'}</span>
      <span data-testid="auth-token">{token || 'NO_TOKEN'}</span>
      <span data-testid="auth-role">{role || 'NO_ROLE'}</span>
      <span data-testid="auth-wallet">{user?.walletAddress || 'NO_WALLET'}</span>
      <button data-testid="logout-btn" onClick={logout}>Logout</button>
    </div>
  );
};

describe('Phase 2 Authentication UI & State Test Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders anonymous status initially', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('auth-status').textContent).toBe('ANONYMOUS');
    expect(screen.getByTestId('auth-token').textContent).toBe('NO_TOKEN');
  });

  it('renders Login Page UI elements correctly', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/E-HealthShield Authentication/i)).toBeInTheDocument();
    expect(screen.getByText(/Sign In with MetaMask/i)).toBeInTheDocument();
    expect(screen.getByText(/Register User/i)).toBeInTheDocument();
  });

  it('allows switching to registration mode and selecting user role', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <LoginPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    fireEvent.click(screen.getByText(/Register User/i));

    expect(screen.getByText(/Select User Role:/i)).toBeInTheDocument();
    expect(screen.getAllByText(/PATIENT/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/DOCTOR/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Register as PATIENT/i)).toBeInTheDocument();
  });
});
