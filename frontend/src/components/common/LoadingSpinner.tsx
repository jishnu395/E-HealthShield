import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ message = 'Loading...' }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '2rem' }}>
      <Loader2 style={{ width: '2rem', height: '2rem', animation: 'spin 1s linear infinite' }} />
      <p style={{ marginTop: '0.75rem', color: '#4b5563', fontSize: '0.9rem' }}>{message}</p>
    </div>
  );
};
