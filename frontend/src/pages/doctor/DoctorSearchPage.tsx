import React, { useState } from 'react';
import { apiClient } from '../../api/client';
import { EhrSearchResultResponse, EhrRetrievalResponse } from '../../types/ehr';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Search, FileText, Download, Lock, CheckCircle, Clock, ShieldAlert } from 'lucide-react';

export const DoctorSearchPage: React.FC = () => {
  const [keyword, setKeyword] = useState<string>('cardiology');
  const [results, setResults] = useState<EhrSearchResultResponse[] | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [retrievedBundle, setRetrievedBundle] = useState<EhrRetrievalResponse | null>(null);
  const [retrievingId, setRetrievingId] = useState<string | null>(null);
  const [retrievalError, setRetrievalError] = useState<string | null>(null);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setRetrievedBundle(null);

    if (!keyword.trim()) {
      setError('Search keyword must not be empty.');
      return;
    }

    setIsLoading(true);

    try {
      const res = await apiClient.post<EhrSearchResultResponse[]>('/api/ehrs/search', {
        keyword: keyword.trim()
      });
      setResults(res.data);
      setIsLoading(false);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Blind keyword search failed';
      setError(msg);
      setIsLoading(false);
    }
  };

  const handleRetrieve = async (recordId: string) => {
    setRetrievalError(null);
    setRetrievingId(recordId);

    try {
      const res = await apiClient.get<EhrRetrievalResponse>(`/api/ehrs/${recordId}/download`);
      setRetrievedBundle(res.data);
      setRetrievingId(null);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Encrypted record retrieval failed';
      setRetrievalError(msg);
      setRetrievingId(null);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Search Console Header */}
      <div style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <Search style={{ color: '#16a34a' }} />
          <h2 style={{ margin: 0, fontSize: '1.3rem', color: '#0f172a' }}>Blind Keyword Search Console</h2>
        </div>
        <p style={{ margin: 0, color: '#64748b', fontSize: '0.875rem' }}>
          Search encrypted database via HMAC-SHA256 trapdoors without exposing search terms or plaintext medical contents.
        </p>
      </div>

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {/* Search Input Bar */}
      <form onSubmit={handleSearch} style={{
        backgroundColor: '#ffffff',
        borderRadius: '0.75rem',
        padding: '1.25rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        display: 'flex',
        gap: '0.75rem'
      }}>
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Enter search keyword (e.g. cardiology, report)..."
          disabled={isLoading}
          style={{
            flex: 1,
            padding: '0.75rem',
            borderRadius: '0.375rem',
            border: '1px solid #cbd5e1',
            fontSize: '1rem'
          }}
        />
        <button
          type="submit"
          disabled={isLoading}
          style={{
            backgroundColor: '#16a34a',
            color: '#ffffff',
            border: 'none',
            padding: '0.75rem 1.5rem',
            borderRadius: '0.375rem',
            fontWeight: '600',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}
        >
          <Search size={18} />
          {isLoading ? 'Searching Trapdoors...' : 'Search'}
        </button>
      </form>

      {/* Search Results List */}
      {results !== null && (
        <div style={{
          backgroundColor: '#ffffff',
          borderRadius: '0.75rem',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ margin: '0 0 1rem 0', fontSize: '1.1rem', color: '#0f172a' }}>
            Search Results ({results.length} record{results.length !== 1 ? 's' : ''} found)
          </h3>

          {results.length === 0 ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b', backgroundColor: '#f8fafc', borderRadius: '0.5rem' }}>
              No matching encrypted records found for trapdoor keyword "<strong>{keyword}</strong>".
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {results.map((rec) => (
                <div
                  key={rec.recordId}
                  style={{
                    border: '1px solid #e2e8f0',
                    borderRadius: '0.5rem',
                    padding: '1rem',
                    backgroundColor: '#f8fafc',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.5rem'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: '600', color: '#0f172a' }}>
                      <FileText size={18} style={{ color: '#0284c7' }} />
                      {rec.fileName} ({rec.contentType})
                    </div>
                    <button
                      onClick={() => handleRetrieve(rec.recordId)}
                      disabled={retrievingId === rec.recordId}
                      style={{
                        backgroundColor: '#0284c7',
                        color: 'white',
                        border: 'none',
                        padding: '0.35rem 0.75rem',
                        borderRadius: '0.25rem',
                        fontSize: '0.8rem',
                        fontWeight: '600',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.25rem'
                      }}
                    >
                      <Download size={14} />
                      {retrievingId === rec.recordId ? 'Checking ACL...' : 'Retrieve Bundle'}
                    </button>
                  </div>

                  <div style={{ fontSize: '0.825rem', color: '#475569', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.5rem' }}>
                    <div><strong>Record ID:</strong> <code style={{ fontSize: '0.75rem' }}>{rec.recordId}</code></div>
                    <div><strong>Patient:</strong> <code style={{ fontSize: '0.75rem' }}>{rec.patientWallet}</code></div>
                    <div><strong>Uploader:</strong> <code style={{ fontSize: '0.75rem' }}>{rec.uploaderWallet}</code></div>
                    <div><strong>File Hash:</strong> <code style={{ fontSize: '0.75rem' }}>{rec.fileHash.substring(0, 16)}...</code></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Retrieval Error Alert */}
      {retrievalError && <ErrorAlert message={retrievalError} onDismiss={() => setRetrievalError(null)} />}

      {/* Retrieved Bundle Metadata Viewer */}
      {retrievedBundle && (
        <div style={{
          backgroundColor: '#f0f9ff',
          border: '1px solid #bae6fd',
          borderRadius: '0.75rem',
          padding: '1.5rem',
          boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#0369a1', marginBottom: '1rem' }}>
            <Lock size={22} />
            <h3 style={{ margin: 0, fontSize: '1.15rem' }}>Encrypted Bundle Retrieved (ACL Authorized)</h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.875rem', color: '#0c4a6e' }}>
            <div><strong>Record ID:</strong> <code>{retrievedBundle.recordId}</code></div>
            <div><strong>Document File:</strong> {retrievedBundle.fileName}</div>
            <div><strong>Encrypted Payload Size:</strong> {retrievedBundle.encryptedCiphertext ? Math.round(retrievedBundle.encryptedCiphertext.length * 0.75) : 0} bytes</div>
            <div><strong>NIST ML-KEM-768 Ciphertext:</strong> {retrievedBundle.kemCiphertext ? `${retrievedBundle.kemCiphertext.length} chars (Base64)` : 'N/A'}</div>
            <div><strong>AES-256-GCM IV:</strong> {retrievedBundle.iv ? `${retrievedBundle.iv.length} chars (Base64)` : 'N/A'}</div>
            <div><strong>SHA-256 Integrity Digest:</strong> <code>{retrievedBundle.fileHash}</code></div>
          </div>
          <p style={{ fontSize: '0.8rem', color: '#0369a1', marginTop: '1rem', marginBottom: 0 }}>
            <em>Note: Content is encrypted. Only the recipient patient holding their ML-KEM-768 private key can perform local client-side decapsulation and decryption.</em>
          </p>
        </div>
      )}
    </div>
  );
};
