import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { EhrSearchResultResponse, EhrRetrievalResponse } from '../../types/ehr';
import { decryptEhrBundle } from '../../utils/cryptoUtils';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { FileText, Shield, ExternalLink, Unlock, Download, X, CheckCircle2, Lock } from 'lucide-react';

export const PatientRecordsPage: React.FC = () => {
  const { user, getPatientPrivateKey } = useAuth();
  const patientWallet = user?.walletAddress || '';

  const [records, setRecords] = useState<EhrSearchResultResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Decryption modal state
  const [decryptingRecordId, setDecryptingRecordId] = useState<string | null>(null);
  const [decryptionError, setDecryptionError] = useState<string | null>(null);
  const [decryptedModal, setDecryptedModal] = useState<{
    fileName: string;
    contentType: string;
    textData: string;
    blobUrl: string;
  } | null>(null);

  useEffect(() => {
    const fetchRecords = async () => {
      if (!patientWallet) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const res = await apiClient.get<EhrSearchResultResponse[]>(`/api/ehrs/patient/${patientWallet}`);
        setRecords(res.data);
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message || 'Failed to fetch patient EHR records';
        setError(msg);
      } finally {
        setIsLoading(false);
      }
    };

    fetchRecords();
  }, [patientWallet]);

  const handleDecrypt = async (recordId: string) => {
    setDecryptingRecordId(recordId);
    setDecryptionError(null);

    try {
      // 1. Load client-side ML-KEM-768 private key
      const privKeyBytes = getPatientPrivateKey();
      if (!privKeyBytes) {
        throw new Error('Patient ML-KEM-768 private key not found in local browser storage.');
      }

      // 2. Fetch encrypted EHR bundle from backend
      const res = await apiClient.get<EhrRetrievalResponse>(
        `/api/ehrs/${recordId}/download?requesterWallet=${patientWallet}`
      );
      const bundle = res.data;

      // 3. Client-side ML-KEM-768 decapsulation + Web Crypto API AES-256-GCM decryption
      const decrypted = await decryptEhrBundle(bundle, privKeyBytes);

      // Create Blob for file preview & download
      const bufferSlice = decrypted.data.buffer.slice(decrypted.data.byteOffset, decrypted.data.byteOffset + decrypted.data.byteLength) as ArrayBuffer;
      const blob = new Blob([bufferSlice], { type: decrypted.contentType });
      const blobUrl = URL.createObjectURL(blob);
      const textDecoder = new TextDecoder('utf-8');
      const textData = textDecoder.decode(decrypted.data);

      setDecryptedModal({
        fileName: decrypted.fileName,
        contentType: decrypted.contentType,
        textData,
        blobUrl
      });
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Decryption failed';
      setDecryptionError(msg);
    } finally {
      setDecryptingRecordId(null);
    }
  };

  const closeDecryptedModal = () => {
    if (decryptedModal?.blobUrl) {
      URL.revokeObjectURL(decryptedModal.blobUrl);
    }
    setDecryptedModal(null);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Banner */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <FileText style={{ color: '#0284c7' }} size={28} />
          <div>
            <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>My Encrypted EHR Records</h2>
            <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.85rem' }}>
              Zero-knowledge encrypted medical records registered under patient wallet <code>{patientWallet}</code>
            </p>
          </div>
        </div>
      </div>

      {/* Phase 5 PQC Decryption Enabled Banner */}
      <div style={{
        backgroundColor: '#f0fdf4',
        borderRadius: '0.5rem',
        padding: '1rem 1.25rem',
        border: '1px solid #bbf7d0',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '0.75rem'
      }}>
        <CheckCircle2 style={{ color: '#16a34a', flexShrink: 0, marginTop: '0.1rem' }} size={20} />
        <div style={{ fontSize: '0.85rem', color: '#166534', lineHeight: '1.4' }}>
          <strong>Post-Quantum Zero-Knowledge Browser Decryption Active:</strong> Click "Decrypt & View" to decapsulate the AES-256 key using your client-side ML-KEM-768 private key and decrypt the document payload via Web Crypto API locally in your browser. Your private key is never sent to the backend.
        </div>
      </div>

      {error && <ErrorAlert message={error} />}
      {decryptionError && <ErrorAlert message={decryptionError} onDismiss={() => setDecryptionError(null)} />}

      {/* Records Table Card */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        {isLoading ? (
          <LoadingSpinner message="Fetching your encrypted medical records..." />
        ) : records.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem', color: '#64748b' }}>
            <Shield size={48} style={{ color: '#cbd5e1', marginBottom: '1rem' }} />
            <p style={{ fontSize: '1rem', fontWeight: '500', margin: 0 }}>No EHR records registered yet.</p>
            <p style={{ fontSize: '0.85rem', marginTop: '0.25rem' }}>
              When healthcare providers upload EHR files for your wallet address, they will appear here.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e2e8f0', color: '#475569' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Record ID</th>
                  <th style={{ padding: '0.75rem 1rem' }}>File Name</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Uploader Doctor</th>
                  <th style={{ padding: '0.75rem 1rem' }}>SHA-256 Checksum</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Uploaded Date</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {records.map((rec) => (
                  <tr key={rec.recordId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                      {rec.recordId.substring(0, 8)}...
                    </td>
                    <td style={{ padding: '0.85rem 1rem', fontWeight: '600', color: '#0f172a' }}>
                      {rec.fileName}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontSize: '0.8rem', color: '#475569' }}>
                      {rec.uploaderWallet ? `${rec.uploaderWallet.substring(0, 6)}...${rec.uploaderWallet.substring(38)}` : 'N/A'}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontSize: '0.75rem', color: '#0284c7' }}>
                      {rec.fileHash ? `${rec.fileHash.substring(0, 12)}...` : 'N/A'}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', color: '#64748b' }}>
                      {new Date(rec.createdAt).toLocaleDateString()}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.75rem' }}>
                        <button
                          onClick={() => handleDecrypt(rec.recordId)}
                          disabled={decryptingRecordId === rec.recordId}
                          style={{
                            backgroundColor: '#0284c7',
                            color: '#ffffff',
                            border: 'none',
                            borderRadius: '0.375rem',
                            padding: '0.4rem 0.75rem',
                            fontSize: '0.8rem',
                            fontWeight: '600',
                            cursor: decryptingRecordId === rec.recordId ? 'not-allowed' : 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem'
                          }}
                        >
                          <Unlock size={14} />
                          {decryptingRecordId === rec.recordId ? 'Decrypting...' : 'Decrypt & View'}
                        </button>

                        <Link
                          to={`/patient/audit?recordId=${rec.recordId}`}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                            color: '#64748b',
                            textDecoration: 'none',
                            fontWeight: '500',
                            fontSize: '0.8rem'
                          }}
                        >
                          Audit <ExternalLink size={14} />
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Decrypted Document Viewer Modal */}
      {decryptedModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem'
        }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '0.75rem',
            maxWidth: '650px',
            width: '100%',
            maxHeight: '85vh',
            display: 'flex',
            flexDirection: 'column',
            boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)'
          }}>
            {/* Modal Header */}
            <div style={{
              padding: '1.25rem 1.5rem',
              borderBottom: '1px solid #e2e8f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Lock style={{ color: '#16a34a' }} size={22} />
                <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#0f172a' }}>
                  Decrypted Medical Document
                </h3>
              </div>
              <button
                onClick={closeDecryptedModal}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Modal Body */}
            <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
              <div style={{ marginBottom: '1rem', fontSize: '0.85rem', color: '#475569' }}>
                <strong>File Name:</strong> {decryptedModal.fileName} | <strong>Type:</strong> {decryptedModal.contentType}
              </div>

              <div style={{
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '0.5rem',
                padding: '1rem',
                fontFamily: 'monospace',
                fontSize: '0.85rem',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                maxHeight: '350px',
                overflowY: 'auto',
                color: '#0f172a'
              }}>
                {decryptedModal.textData}
              </div>
            </div>

            {/* Modal Footer */}
            <div style={{
              padding: '1rem 1.5rem',
              borderTop: '1px solid #e2e8f0',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '0.75rem'
            }}>
              <a
                href={decryptedModal.blobUrl}
                download={decryptedModal.fileName}
                style={{
                  backgroundColor: '#16a34a',
                  color: '#ffffff',
                  padding: '0.5rem 1rem',
                  borderRadius: '0.375rem',
                  textDecoration: 'none',
                  fontSize: '0.875rem',
                  fontWeight: '600',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.35rem'
                }}
              >
                <Download size={16} /> Download Decrypted File
              </a>
              <button
                onClick={closeDecryptedModal}
                style={{
                  backgroundColor: '#e2e8f0',
                  color: '#334155',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  borderRadius: '0.375rem',
                  fontSize: '0.875rem',
                  fontWeight: '500',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
