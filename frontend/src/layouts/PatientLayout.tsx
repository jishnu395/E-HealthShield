import React from 'react';
import { Outlet } from 'react-router-dom';

export const PatientLayout: React.FC = () => {
  return (
    <div className="patient-layout">
      <div style={{ marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid #e2e8f0' }}>
        <h2 style={{ color: '#0f172a', margin: 0 }}>Patient Workspace</h2>
        <small style={{ color: '#64748b' }}>Data Owner Security & Local Post-Quantum Decryption Portal</small>
      </div>
      <Outlet />
    </div>
  );
};
