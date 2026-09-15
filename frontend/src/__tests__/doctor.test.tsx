import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import React from 'react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { WalletProvider } from '../context/WalletContext';
import { DoctorUploadPage } from '../pages/doctor/DoctorUploadPage';
import { DoctorSearchPage } from '../pages/doctor/DoctorSearchPage';
import { DoctorDashboardPage } from '../pages/doctor/DoctorDashboardPage';

describe('Doctor Workspace & EHR Pipeline Test Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders Doctor Dashboard overview correctly', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <DoctorDashboardPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Doctor Clinical Workspace/i)).toBeInTheDocument();
    expect(screen.getByText(/Upload EHR/i)).toBeInTheDocument();
    expect(screen.getByText(/Blind Keyword Search/i)).toBeInTheDocument();
  });

  it('validates upload inputs and prevents empty uploads', async () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <DoctorUploadPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Ingest & Upload Encrypted EHR/i)).toBeInTheDocument();

    // Submit without file
    fireEvent.click(screen.getByRole('button', { name: /Upload & Anchor EHR/i }));

    await waitFor(() => {
      expect(screen.getByText(/Please select an EHR file to upload/i)).toBeInTheDocument();
    });
  });

  it('derives uploader wallet as read-only field', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <DoctorUploadPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    const uploaderInput = screen.getByLabelText(/Uploader Doctor Wallet/i) as HTMLInputElement;
    expect(uploaderInput).toBeInTheDocument();
    expect(uploaderInput).toHaveAttribute('readonly');
  });

  it('renders Blind Search console with keyword input', () => {
    render(
      <MemoryRouter>
        <WalletProvider>
          <AuthProvider>
            <DoctorSearchPage />
          </AuthProvider>
        </WalletProvider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Blind Keyword Search Console/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter search keyword/i)).toBeInTheDocument();
  });
});
