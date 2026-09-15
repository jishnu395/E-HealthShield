import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { EhrSearchResultResponse } from '../../types/ehr';
import { FileText, Search, Shield, CheckCircle } from 'lucide-react';

export const DoctorRecordsPage: React.FC = () => {
  const { user } = useAuth();
  const [records, setRecords] = useState<EhrSearchResultResponse[]>([]);
  const [hasLoaded, setHasLoaded] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const loadAuthorizedRecords = async () => {
    setIsLoading(true);
    try {
      // Query search endpoint for doctor's records
      const res = await apiClient.post<EhrSearchResultResponse[]>('/api/ehrs/search', {
        keyword: 'report'
      });
      setRecords(res.data);
      setHasLoaded(true);
    } catch (err) {
      console.warn('Failed to load authorized records:', err);
    } finally {
      setIsLoading(false);
    }
  };

  React.useEffect(() => {
    loadAuthorizedRecords();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <FileText style={{ color: '#8b5cf6' }} />
            <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>Authorized Record Registry</h2>
          </div>
          <p style={{ margin: 0, color: '#64748b', fontSize: '0.875rem' }}>
            Records uploaded by or granted access to Doctor <code>{user?.walletAddress}</code> on Ethereum Smart Contract.
          </p>
        </div>
        <button
          onClick={loadAuthorizedRecords}
          disabled={isLoading}
          style={{
            backgroundColor: '#8b5cf6',
            color: 'white',
            border: 'none',
            padding: '0.5rem 1rem',
            borderRadius: '0.375rem',
            fontWeight: '600',
            cursor: 'pointer'
          }}
        >
          {isLoading ? 'Refreshing...' : 'Refresh Registry'}
        </button>
      </div>

      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        {records.length === 0 ? (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '0.5rem' }}>
            No authorized records currently retrieved in registry. Use the Blind Search or Upload tab to ingest new records.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {records.map((rec) => (
              <div
                key={rec.recordId}
                style={{
                  border: '1px solid #e2e8f0',
                  borderRadius: '0.5rem',
                  padding: '1rem',
                  backgroundColor: '#f8fafc',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <div>
                  <div style={{ fontWeight: '600', color: '#0f172a' }}>{rec.fileName}</div>
                  <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '0.25rem' }}>
                    Record ID: <code>{rec.recordId}</code> | Patient: <code>{rec.patientWallet.substring(0, 8)}...</code>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#16a34a' }}>
                  <CheckCircle size={16} />
                  On-Chain Anchored
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
