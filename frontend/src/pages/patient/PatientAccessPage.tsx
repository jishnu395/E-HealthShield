import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { AccessPermissionResponse } from '../../types/access';
import { EhrSearchResultResponse } from '../../types/ehr';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { UserCheck, UserX, Shield, CheckCircle, AlertCircle, Clock, Key } from 'lucide-react';

export const PatientAccessPage: React.FC = () => {
  const { user } = useAuth();
  const patientWallet = user?.walletAddress || '';

  const [permissions, setPermissions] = useState<AccessPermissionResponse[]>([]);
  const [patientRecords, setPatientRecords] = useState<EhrSearchResultResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form State
  const [selectedRecordId, setSelectedRecordId] = useState<string>('');
  const [doctorWalletInput, setDoctorWalletInput] = useState<string>('0x90F79bf6EB2c4f870365E785982E1f101E93b906');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const fetchPermissionsAndRecords = async () => {
    if (!patientWallet) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const [permsRes, recordsRes] = await Promise.all([
        apiClient.get<AccessPermissionResponse[]>(`/api/access/patient/${patientWallet}`),
        apiClient.get<EhrSearchResultResponse[]>(`/api/ehrs/patient/${patientWallet}`).catch(() => ({ data: [] }))
      ]);
      setPermissions(permsRes.data);
      setPatientRecords(recordsRes.data);
      if (recordsRes.data.length > 0 && !selectedRecordId) {
        setSelectedRecordId(recordsRes.data[0].recordId);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to fetch access permissions';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissionsAndRecords();
  }, [patientWallet]);

  const handleGrantAccess = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!selectedRecordId.trim()) {
      setError('Please select or enter a valid Record ID.');
      return;
    }

    if (!doctorWalletInput.trim() || !/^0x[a-fA-F0-9]{40}$/.test(doctorWalletInput.trim())) {
      setError('Invalid Doctor Wallet address format (must be 0x followed by 40 hexadecimal characters).');
      return;
    }

    setIsSubmitting(true);

    try {
      const res = await apiClient.post<AccessPermissionResponse>('/api/access/grant', {
        recordId: selectedRecordId.trim(),
        doctorWallet: doctorWalletInput.trim()
      });

      setSuccessMessage(`Access GRANTED successfully for Doctor ${doctorWalletInput.trim().substring(0, 8)}... on-chain.`);
      await fetchPermissionsAndRecords();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to grant access';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRevokeAccess = async (recordIdToRevoke: string, doctorWalletToRevoke: string) => {
    setError(null);
    setSuccessMessage(null);
    setIsSubmitting(true);

    try {
      await apiClient.post<AccessPermissionResponse>('/api/access/revoke', {
        recordId: recordIdToRevoke,
        doctorWallet: doctorWalletToRevoke
      });

      setSuccessMessage(`Access REVOKED successfully for Doctor ${doctorWalletToRevoke.substring(0, 8)}... on-chain.`);
      await fetchPermissionsAndRecords();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to revoke access';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

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
          <Shield style={{ color: '#16a34a' }} size={28} />
          <div>
            <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>Access Control List (ACL) Governance</h2>
            <p style={{ margin: '0.25rem 0 0 0', color: '#64748b', fontSize: '0.85rem' }}>
              Grant or revoke doctor access permissions on the Ethereum smart contract for patient wallet <code>{patientWallet}</code>
            </p>
          </div>
        </div>
      </div>

      {error && <ErrorAlert message={error} />}

      {successMessage && (
        <div style={{
          backgroundColor: '#f0fdf4',
          borderRadius: '0.5rem',
          padding: '1rem 1.25rem',
          border: '1px solid #bbf7d0',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          color: '#166534',
          fontSize: '0.9rem',
          fontWeight: '500'
        }}>
          <CheckCircle size={20} style={{ color: '#16a34a', flexShrink: 0 }} />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Action Form Card */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.1rem', color: '#0f172a' }}>Grant Doctor Access Permission</h3>

        <form onSubmit={handleGrantAccess} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', color: '#334155', marginBottom: '0.35rem' }}>
                Select EHR Record ID:
              </label>
              {patientRecords.length > 0 ? (
                <select
                  value={selectedRecordId}
                  onChange={(e) => setSelectedRecordId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.6rem 0.75rem',
                    borderRadius: '0.375rem',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.85rem',
                    backgroundColor: '#ffffff'
                  }}
                >
                  {patientRecords.map((r) => (
                    <option key={r.recordId} value={r.recordId}>
                      {r.fileName} ({r.recordId.substring(0, 8)}...)
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
                    width: '100%',
                    padding: '0.6rem 0.75rem',
                    borderRadius: '0.375rem',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.85rem'
                  }}
                />
              )}
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: '600', color: '#334155', marginBottom: '0.35rem' }}>
                Doctor Ethereum Wallet Address:
              </label>
              <input
                type="text"
                placeholder="0x..."
                value={doctorWalletInput}
                onChange={(e) => setDoctorWalletInput(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.6rem 0.75rem',
                  borderRadius: '0.375rem',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.85rem',
                  fontFamily: 'monospace'
                }}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              alignSelf: 'flex-start',
              backgroundColor: '#16a34a',
              color: '#ffffff',
              border: 'none',
              padding: '0.65rem 1.5rem',
              borderRadius: '0.375rem',
              fontWeight: '600',
              fontSize: '0.9rem',
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              opacity: isSubmitting ? 0.7 : 1
            }}
          >
            <UserCheck size={18} />
            {isSubmitting ? 'Syncing On-Chain Grant...' : 'Grant Access Permission'}
          </button>
        </form>
      </div>

      {/* Permissions List Table */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.1rem', color: '#0f172a' }}>Active & Historical Access Grants</h3>

        {isLoading ? (
          <LoadingSpinner message="Loading access control list..." />
        ) : permissions.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: '#64748b' }}>
            <Key size={40} style={{ color: '#cbd5e1', marginBottom: '0.75rem' }} />
            <p style={{ margin: 0, fontSize: '0.95rem', fontWeight: '500' }}>No access permissions granted yet.</p>
            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem' }}>
              Use the form above to grant doctor wallet addresses access to your EHR records.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e2e8f0', color: '#475569' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Record ID</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Doctor Wallet</th>
                  <th style={{ padding: '0.75rem 1rem' }}>ACL Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Last Updated</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'center' }}>Revoke Control</th>
                </tr>
              </thead>
              <tbody>
                {permissions.map((p) => {
                  const isGranted = p.isGranted ?? (p as any).granted;
                  return (
                    <tr key={p.id || `${p.recordId}-${p.doctorWallet}`} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        {p.recordId.substring(0, 8)}...
                      </td>
                      <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontSize: '0.85rem', color: '#0f172a', fontWeight: '500' }}>
                        {p.doctorWallet}
                      </td>
                      <td style={{ padding: '0.85rem 1rem' }}>
                        {isGranted ? (
                          <span style={{
                            backgroundColor: '#dcfce7',
                            color: '#15803d',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '1rem',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem'
                          }}>
                            <CheckCircle size={12} /> GRANTED
                          </span>
                        ) : (
                          <span style={{
                            backgroundColor: '#fee2e2',
                            color: '#b91c1c',
                            padding: '0.25rem 0.6rem',
                            borderRadius: '1rem',
                            fontSize: '0.75rem',
                            fontWeight: '600',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem'
                          }}>
                            <UserX size={12} /> REVOKED
                          </span>
                        )}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', color: '#64748b', fontSize: '0.8rem' }}>
                        {p.updatedAt ? new Date(p.updatedAt).toLocaleString() : 'N/A'}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                        {isGranted ? (
                          <button
                            onClick={() => handleRevokeAccess(p.recordId, p.doctorWallet)}
                            disabled={isSubmitting}
                            style={{
                              backgroundColor: '#ef4444',
                              color: '#ffffff',
                              border: 'none',
                              padding: '0.35rem 0.75rem',
                              borderRadius: '0.375rem',
                              fontSize: '0.75rem',
                              fontWeight: '600',
                              cursor: 'pointer'
                            }}
                          >
                            Revoke Access
                          </button>
                        ) : (
                          <span style={{ color: '#94a3b8', fontSize: '0.75rem' }}>Revoked</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
