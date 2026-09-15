import React from 'react';
import { Outlet } from 'react-router-dom';

export const DoctorLayout: React.FC = () => {
  return (
    <div className="doctor-layout">
      <div style={{ marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid #e2e8f0' }}>
        <h2 style={{ color: '#0f172a', margin: 0 }}>Doctor Console</h2>
        <small style={{ color: '#64748b' }}>Healthcare Provider EHR Ingestion & Blind Search Console</small>
      </div>
      <Outlet />
    </div>
  );
};
