import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { EhrSearchResultResponse } from '../../types/ehr';
import { Upload, Search, FileText, Shield, CheckCircle, Clock } from 'lucide-react';

export const DoctorDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [recentRecords, setRecentRecords] = useState<EhrSearchResultResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    // Fetch initial sample records for doctor's workspace view
    const fetchRecent = async () => {
      try {
        const res = await apiClient.post<EhrSearchResultResponse[]>('/api/ehrs/search', {
          keyword: 'general'
        });
        setRecentRecords(res.data);
      } catch (err) {
        // Fallback silently if search keyword returns empty
      } finally {
        setIsLoading(false);
      }
    };

    fetchRecent();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Profile Card */}
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
            <Shield style={{ color: '#0284c7' }} />
            <h2 style={{ margin: 0, fontSize: '1.4rem', color: '#0f172a' }}>Doctor Clinical Workspace</h2>
          </div>
          <p style={{ margin: 0, color: '#64748b', fontSize: '0.9rem' }}>
            Authenticated Provider Wallet: <code style={{ backgroundColor: '#f1f5f9', padding: '0.2rem 0.4rem', borderRadius: '0.25rem' }}>{user?.walletAddress}</code>
          </p>
        </div>
        <div style={{ backgroundColor: '#f0f9ff', color: '#0369a1', padding: '0.5rem 1rem', borderRadius: '2rem', fontSize: '0.85rem', fontWeight: '600' }}>
          Role: DOCTOR
        </div>
      </div>

      {/* Quick Action Navigation Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
        <Link to="/doctor/upload" style={{ textDecoration: 'none' }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '0.75rem',
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            borderLeft: '4px solid #0284c7',
            transition: 'transform 0.1s ease',
            cursor: 'pointer'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <Upload style={{ color: '#0284c7' }} />
              <h3 style={{ margin: 0, color: '#0f172a', fontSize: '1.1rem' }}>Upload EHR</h3>
            </div>
            <p style={{ margin: 0, color: '#64748b', fontSize: '0.85rem' }}>
              Ingest new medical records with hybrid post-quantum encryption & blind SSE indexing.
            </p>
          </div>
        </Link>

        <Link to="/doctor/search" style={{ textDecoration: 'none' }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '0.75rem',
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            borderLeft: '4px solid #16a34a',
            cursor: 'pointer'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <Search style={{ color: '#16a34a' }} />
              <h3 style={{ margin: 0, color: '#0f172a', fontSize: '1.1rem' }}>Blind Keyword Search</h3>
            </div>
            <p style={{ margin: 0, color: '#64748b', fontSize: '0.85rem' }}>
              Search encrypted patient records via HMAC-SHA256 trapdoors without exposing terms.
            </p>
          </div>
        </Link>

        <Link to="/doctor/records" style={{ textDecoration: 'none' }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '0.75rem',
            padding: '1.5rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            borderLeft: '4px solid #8b5cf6',
            cursor: 'pointer'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <FileText style={{ color: '#8b5cf6' }} />
              <h3 style={{ margin: 0, color: '#0f172a', fontSize: '1.1rem' }}>Record Registry</h3>
            </div>
            <p style={{ margin: 0, color: '#64748b', fontSize: '0.85rem' }}>
              View authorized records where doctor wallet has smart contract ACL permission.
            </p>
          </div>
        </Link>
      </div>

      {/* System Status Banner */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.1rem', color: '#0f172a' }}>Active Cryptographic Status</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          <div style={{ backgroundColor: '#f8fafc', padding: '0.75rem 1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Public Key KEM Standard</div>
            <div style={{ fontSize: '0.95rem', fontWeight: '600', color: '#0f172a', marginTop: '0.25rem' }}>NIST FIPS 203 ML-KEM-768</div>
          </div>
          <div style={{ backgroundColor: '#f8fafc', padding: '0.75rem 1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Symmetric Payload Cipher</div>
            <div style={{ fontSize: '0.95rem', fontWeight: '600', color: '#0f172a', marginTop: '0.25rem' }}>AES-256-GCM</div>
          </div>
          <div style={{ backgroundColor: '#f8fafc', padding: '0.75rem 1rem', borderRadius: '0.5rem', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Smart Contract Anchor</div>
            <div style={{ fontSize: '0.95rem', fontWeight: '600', color: '#0f172a', marginTop: '0.25rem' }}>EHealthShieldACL</div>
          </div>
        </div>
      </div>
    </div>
  );
};
