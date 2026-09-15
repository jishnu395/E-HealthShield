import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { WalletProvider } from '../context/WalletContext';
import { PatientDashboardPage } from '../pages/patient/PatientDashboardPage';
import { PatientRecordsPage } from '../pages/patient/PatientRecordsPage';
import { PatientAccessPage } from '../pages/patient/PatientAccessPage';
import { PatientAuditPage } from '../pages/patient/PatientAuditPage';

describe('Phase 4 Patient Workspace & ACL Governance Test Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders Patient Dashboard overview correctly', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <PatientDashboardPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Patient Health Dashboard/i)).toBeInTheDocument();
    expect(screen.getByText(/My Encrypted Records/i)).toBeInTheDocument();
    expect(screen.getByText(/ACL Access Governance/i)).toBeInTheDocument();
    expect(screen.getByText(/Blockchain Audit Feed/i)).toBeInTheDocument();
  });

  it('renders Patient Records page with Phase 5 PQC decryption notice', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <PatientRecordsPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/My Encrypted EHR Records/i)).toBeInTheDocument();
    expect(screen.getByText(/Post-Quantum Zero-Knowledge Browser Decryption Active/i)).toBeInTheDocument();
  });

  it('renders Patient Access Governance page with Grant form and doctor wallet input', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <PatientAccessPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Access Control List \(ACL\) Governance/i)).toBeInTheDocument();
    expect(screen.getByText(/Grant Doctor Access Permission/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Grant Access Permission/i })).toBeInTheDocument();
  });

  it('renders Patient Audit page with run audit verification trigger', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <PatientAuditPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Immutable Blockchain Audit Feed/i)).toBeInTheDocument();
    expect(screen.getByText(/Select EHR Record for Integrity Audit/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Run Audit Verification/i })).toBeInTheDocument();
  });
});
