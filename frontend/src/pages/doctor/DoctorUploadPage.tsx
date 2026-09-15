import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { EhrUploadResponse } from '../../types/ehr';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Upload, FileText, CheckCircle, Shield, Tag, AlertCircle } from 'lucide-react';

export const DoctorUploadPage: React.FC = () => {
  const { user } = useAuth();
  const uploaderWallet = user?.walletAddress || '';

  const [file, setFile] = useState<File | null>(null);
  const [patientWallet, setPatientWallet] = useState<string>('0x70997970C51812dc3A010C7d01b50e0d17dc79C8');
  const [keywordInput, setKeywordInput] = useState<string>('cardiology, hypertension, report');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<EhrUploadResponse | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setUploadResult(null);

    // 1. Validation
    if (!file) {
      setError('Please select an EHR file to upload.');
      return;
    }
    if (!patientWallet.trim() || !/^0x[a-fA-F0-9]{40}$/.test(patientWallet.trim())) {
      setError('Invalid patient wallet address format (must be 0x followed by 40 hexadecimal characters).');
      return;
    }
    if (!uploaderWallet || !/^0x[a-fA-F0-9]{40}$/.test(uploaderWallet)) {
      setError('Authenticated doctor wallet is missing or invalid.');
      return;
    }

    const keywords = keywordInput
      .split(',')
      .map(k => k.trim())
      .filter(k => k.length > 0);

    if (keywords.length === 0) {
      setError('At least one search keyword is required for blind SSE indexing.');
      return;
    }

    if (keywords.length > 50) {
      setError('Maximum keyword limit is 50 keywords.');
      return;
    }

    setIsLoading(true);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('patientWallet', patientWallet.trim());
      formData.append('uploaderWallet', uploaderWallet.trim());
      keywords.forEach(k => formData.append('keywords', k));

      const res = await apiClient.post<EhrUploadResponse>('/api/ehrs/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      setUploadResult(res.data);
      setIsLoading(false);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'EHR Upload failed';
      setError(msg);
      setIsLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '750px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <Upload style={{ color: '#0284c7' }} />
          <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>Ingest & Upload Encrypted EHR</h2>
        </div>
        <p style={{ margin: 0, color: '#64748b', fontSize: '0.875rem' }}>
          Encrypt raw clinical documentation using the patient's ML-KEM-768 public key + AES-256-GCM and anchor SHA-256 digest on Ethereum smart contract.
        </p>
      </div>

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {/* Upload Form */}
      <form onSubmit={handleUpload} style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem'
      }}>
        {/* File Selector */}
        <div>
          <label htmlFor="fileInput" style={{ display: 'block', fontWeight: '600', fontSize: '0.875rem', color: '#1e293b', marginBottom: '0.5rem' }}>
            Select EHR Document File:
          </label>
          <input
            id="fileInput"
            type="file"
            onChange={handleFileChange}
            disabled={isLoading}
            style={{
              width: '100%',
              padding: '0.5rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              backgroundColor: '#f8fafc'
            }}
          />
          {file && (
            <div style={{ fontSize: '0.8rem', color: '#0369a1', marginTop: '0.25rem' }}>
              Selected File: <strong>{file.name}</strong> ({Math.round(file.size / 1024)} KB)
            </div>
          )}
        </div>

        {/* Patient Wallet */}
        <div>
          <label htmlFor="patientWallet" style={{ display: 'block', fontWeight: '600', fontSize: '0.875rem', color: '#1e293b', marginBottom: '0.5rem' }}>
            Patient Ethereum Wallet Address (Recipient Owner):
          </label>
          <input
            id="patientWallet"
            type="text"
            value={patientWallet}
            onChange={(e) => setPatientWallet(e.target.value)}
            placeholder="0x..."
            disabled={isLoading}
            style={{
              width: '100%',
              padding: '0.625rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.9rem',
              fontFamily: 'monospace'
            }}
          />
        </div>

        {/* Uploader Wallet (Derived & Read-Only) */}
        <div>
          <label htmlFor="uploaderWallet" style={{ display: 'block', fontWeight: '600', fontSize: '0.875rem', color: '#1e293b', marginBottom: '0.5rem' }}>
            Uploader Doctor Wallet (Derived from Authenticated Web3 Session):
          </label>
          <input
            id="uploaderWallet"
            type="text"
            value={uploaderWallet}
            readOnly
            disabled
            style={{
              width: '100%',
              padding: '0.625rem',
              borderRadius: '0.375rem',
              border: '1px solid #e2e8f0',
              backgroundColor: '#f1f5f9',
              fontSize: '0.9rem',
              fontFamily: 'monospace',
              color: '#475569'
            }}
          />
        </div>

        {/* Keywords */}
        <div>
          <label htmlFor="keywords" style={{ display: 'block', fontWeight: '600', fontSize: '0.875rem', color: '#1e293b', marginBottom: '0.5rem' }}>
            Clinical Keywords (Comma Separated for Blind SSE Trapdoors):
          </label>
          <input
            id="keywords"
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="e.g. cardiology, blood test, 2026 report"
            disabled={isLoading}
            style={{
              width: '100%',
              padding: '0.625rem',
              borderRadius: '0.375rem',
              border: '1px solid #cbd5e1',
              fontSize: '0.9rem'
            }}
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          style={{
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
            marginTop: '0.5rem'
          }}
        >
          <Upload size={20} />
          {isLoading ? 'Encrypting & Anchoring On-Chain...' : 'Upload & Anchor EHR'}
        </button>
      </form>

      {/* Success Card */}
      {uploadResult && (
        <div style={{
          backgroundColor: '#f0fdf4',
          border: '1px solid #bbf7d0',
          borderRadius: '0.75rem',
          padding: '1.5rem',
          boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#15803d', marginBottom: '1rem' }}>
            <CheckCircle size={24} />
            <h3 style={{ margin: 0, fontSize: '1.2rem' }}>EHR Uploaded & Anchored On-Chain Successfully</h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.9rem', color: '#166534' }}>
            <div><strong>Record ID:</strong> <code style={{ backgroundColor: '#dcfce7', padding: '0.1rem 0.3rem', borderRadius: '0.2rem' }}>{uploadResult.recordId}</code></div>
            <div><strong>File Name:</strong> {uploadResult.fileName}</div>
            <div><strong>Content Type:</strong> {uploadResult.contentType}</div>
            <div><strong>Patient Wallet:</strong> <code style={{ backgroundColor: '#dcfce7', padding: '0.1rem 0.3rem', borderRadius: '0.2rem' }}>{uploadResult.patientWallet}</code></div>
            <div><strong>Ciphertext SHA-256 Digest:</strong> <code style={{ wordBreak: 'break-all', backgroundColor: '#dcfce7', padding: '0.1rem 0.3rem', borderRadius: '0.2rem' }}>{uploadResult.fileHash}</code></div>
            <div><strong>Timestamp:</strong> {new Date(uploadResult.createdAt).toLocaleString()}</div>
          </div>
        </div>
      )}
    </div>
  );
};
