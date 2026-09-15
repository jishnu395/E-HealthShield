import React from 'react';
import { AlertCircle } from 'lucide-react';

interface ErrorAlertProps {
  message: string;
  onDismiss?: () => void;
}

export const ErrorAlert: React.FC<ErrorAlertProps> = ({ message, onDismiss }) => {
  return (
    <div style={{
      backgroundColor: '#fef2f2',
      borderLeft: '4px solid #ef4444',
      padding: '1rem',
      margin: '1rem 0',
      borderRadius: '0.375rem',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <AlertCircle style={{ color: '#ef4444', marginRight: '0.75rem', flexShrink: 0 }} />
        <span style={{ color: '#991b1b', fontSize: '0.9rem' }}>{message}</span>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          style={{ background: 'none', border: 'none', color: '#991b1b', cursor: 'pointer', fontWeight: 'bold' }}
        >
          &times;
        </button>
      )}
    </div>
  );
};
