import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { AuditVerificationResponse, OnChainRecordMetadata } from '../../types/audit';
import { EhrSearchResultResponse } from '../../types/ehr';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Activity, ShieldCheck, ShieldAlert, CheckCircle, Clock, Hash, Link as LinkIcon, ExternalLink } from 'lucide-react';

export const PatientAuditPage: React.FC = () => {
  const { user } = useAuth();
  const patientWallet = user?.walletAddress || '';
  const [searchParams] = useSearchParams();
  const initialRecordId = searchParams.get('recordId') || '';

  const [patientRecords, setPatientRecords] = useState<EhrSearchResultResponse[]>([]);
  const [selectedRecordId, setSelectedRecordId] = useState<string>(initialRecordId);
  const [auditResult, setAuditResult] = useState<AuditVerificationResponse | null>(null);
  const [onChainMetadata, setOnChainMetadata] = useState<OnChainRecordMetadata | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPatientRecords = async () => {
      if (!patientWallet) return;
      try {
        const res = await apiClient.get<EhrSearchResultResponse[]>(`/api/ehrs/patient/${patientWallet}`);
        setPatientRecords(res.data);
        if (!selectedRecordId && res.data.length > 0) {
          setSelectedRecordId(res.data[0].recordId);
        }
      } catch (err) {
        // Fallback
      }
    };

    fetchPatientRecords();
  }, [patientWallet]);

  const handleVerify = async (recordIdToVerify?: string) => {
    const recId = recordIdToVerify || selectedRecordId;
    setError(null);
    setAuditResult(null);
    setOnChainMetadata(null);

    if (!recId || !recId.trim()) {
      setError('Please select or enter a valid Record ID to conduct audit verification.');
      return;
    }

    setIsLoading(true);

    try {
      const [verifyRes, metaRes] = await Promise.all([
        apiClient.get<AuditVerificationResponse>(`/api/audit/verify/${recId.trim()}`),
        apiClient.get<OnChainRecordMetadata>(`/api/audit/record/${recId.trim()}`)
      ]);

      setAuditResult(verifyRes.data);
      setOnChainMetadata(metaRes.data);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Audit verification query failed';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (initialRecordId) {
      handleVerify(initialRecordId);
    }
  }, [initialRecordId]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Activity style={{ color: '#8b5cf6' }} size={28} />
          <div>
            <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>Immutable Blockchain Audit Feed</h2>
            <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.85rem' }}>
              Conduct end-to-end cryptographic SHA-256 integrity verification against Ethereum Sepolia on-chain hash anchors.
            </p>
          </div>
        </div>
      </div>

      {error && <ErrorAlert message={error} />}

      {/* Query Selector Form */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.1rem', color: '#0f172a' }}>Select EHR Record for Integrity Audit</h3>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          {patientRecords.length > 0 ? (
            <select
              value={selectedRecordId}
              onChange={(e) => setSelectedRecordId(e.target.value)}
              style={{
                flex: 1,
                minWidth: '280px',
                padding: '0.65rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.85rem'
              }}
            >
              <option value="">-- Select Registered EHR Record --</option>
              {patientRecords.map((r) => (
                <option key={r.recordId} value={r.recordId}>
                  {r.fileName} ({r.recordId})
                </option>
              ))}
            </select>
          ) : (
            <input
              type="text"
              placeholder="Enter Record UUID"
              value={selectedRecordId}
              onChange={(e) => setSelectedRecordId(e.target.value)}
              style={{
                flex: 1,
                minWidth: '280px',
                padding: '0.65rem 0.75rem',
                borderRadius: '0.375rem',
                border: '1px solid #cbd5e1',
                fontSize: '0.85rem'
              }}
            />
          )}

          <button
            onClick={() => handleVerify()}
            disabled={isLoading}
            style={{
              backgroundColor: '#8b5cf6',
              color: '#ffffff',
              border: 'none',
              padding: '0.65rem 1.5rem',
              borderRadius: '0.375rem',
              fontWeight: '600',
              fontSize: '0.9rem',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}
          >
            <ShieldCheck size={18} />
            {isLoading ? 'Verifying Hashes...' : 'Run Audit Verification'}
          </button>
        </div>
      </div>

      {isLoading && <LoadingSpinner message="Verifying database ciphertext digest against Ethereum Sepolia smart contract..." />}

      {/* Audit Verification Report Card */}
      {auditResult && (
        <div style={{
          backgroundColor: '#ffffff',
          borderRadius: '0.75rem',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          borderLeft: auditResult.isTamperFree ? '6px solid #16a34a' : '6px solid #dc2626'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              {auditResult.isTamperFree ? (
                <ShieldCheck style={{ color: '#16a34a' }} size={32} />
              ) : (
                <ShieldAlert style={{ color: '#dc2626' }} size={32} />
              )}
              <div>
                <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#0f172a' }}>
                  {auditResult.isTamperFree ? 'Cryptographic Audit Result: TAMPER-FREE' : 'Cryptographic Audit Result: INTEGRITY MISMATCH'}
                </h3>
                <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.85rem' }}>
                  Record ID: <code>{auditResult.recordId}</code>
                </p>
              </div>
            </div>

            <div style={{
              backgroundColor: auditResult.isTamperFree ? '#dcfce7' : '#fee2e2',
              color: auditResult.isTamperFree ? '#15803d' : '#b91c1c',
              padding: '0.5rem 1rem',
              borderRadius: '2rem',
              fontSize: '0.85rem',
              fontWeight: '700'
            }}>
              {auditResult.isTamperFree ? '100% VERIFIED ON-CHAIN' : 'TAMPER DETECTED'}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
            <div style={{ backgroundColor: '#f8fafc', padding: '1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: '600' }}>
                Local Ciphertext SHA-256 Digest
              </div>
              <div style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: '#0f172a', marginTop: '0.35rem', wordBreak: 'break-all' }}>
                {auditResult.localCiphertextHash}
              </div>
            </div>

            <div style={{ backgroundColor: '#f8fafc', padding: '1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: '600' }}>
                Ethereum On-Chain Anchored Hash
              </div>
              <div style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: '#0284c7', marginTop: '0.35rem', wordBreak: 'break-all' }}>
                {auditResult.onChainAnchoredHash}
              </div>
            </div>
          </div>

          <div style={{ marginTop: '1.25rem', paddingTop: '1.25rem', borderTop: '1px solid #f1f5f9', display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.85rem', color: '#475569' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Hash size={16} style={{ color: '#0284c7' }} />
              <span>Blockchain Transaction Hash:</span>
              <code style={{ backgroundColor: '#f1f5f9', padding: '0.15rem 0.35rem', borderRadius: '0.25rem', color: '#0f172a' }}>
                {auditResult.blockchainTransactionHash || 'Anchored On-Chain'}
              </code>
            </div>

            {onChainMetadata && (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Clock size={16} style={{ color: '#8b5cf6' }} />
                  <span>On-Chain Registration Timestamp:</span>
                  <span style={{ fontWeight: '500', color: '#0f172a' }}>
                    {new Date(auditResult.onChainTimestamp * 1000).toLocaleString()}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <LinkIcon size={16} style={{ color: '#16a34a' }} />
                  <span>Registered Patient Owner:</span>
                  <code style={{ backgroundColor: '#f1f5f9', padding: '0.15rem 0.35rem', borderRadius: '0.25rem' }}>
                    {onChainMetadata.ownerPatient}
                  </code>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
