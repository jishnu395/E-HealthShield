import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Shield, LogOut, User, Stethoscope } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { isAuthenticated, user, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav style={{
      backgroundColor: '#1e293b',
      color: '#ffffff',
      padding: '0.75rem 1.5rem',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <Shield style={{ color: '#38bdf8' }} />
        <span style={{ fontWeight: 'bold', fontSize: '1.2rem', letterSpacing: '0.5px' }}>E-HealthShield</span>
      </div>

      {isAuthenticated && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          {role === 'PATIENT' && (
            <>
              <Link to="/patient" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Dashboard</Link>
              <Link to="/patient/records" style={{ color: '#e2e8f0', textDecoration: 'none' }}>My Records</Link>
              <Link to="/patient/access" style={{ color: '#e2e8f0', textDecoration: 'none' }}>ACL Management</Link>
              <Link to="/patient/audit" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Audit Feed</Link>
            </>
          )}

          {role === 'DOCTOR' && (
            <>
              <Link to="/doctor" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Dashboard</Link>
              <Link to="/doctor/upload" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Upload EHR</Link>
              <Link to="/doctor/search" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Blind Search</Link>
              <Link to="/doctor/records" style={{ color: '#e2e8f0', textDecoration: 'none' }}>Record Registry</Link>
            </>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', backgroundColor: '#334155', padding: '0.25rem 0.75rem', borderRadius: '1rem' }}>
            {role === 'PATIENT' ? <User size={14} /> : <Stethoscope size={14} />}
            <span>{role} ({user?.walletAddress ? `${user.walletAddress.substring(0, 6)}...${user.walletAddress.substring(38)}` : ''})</span>
          </div>

          <button
            onClick={handleLogout}
            style={{
              backgroundColor: '#ef4444',
              color: 'white',
              border: 'none',
              padding: '0.35rem 0.75rem',
              borderRadius: '0.375rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.25rem'
            }}
          >
            <LogOut size={14} />
            Logout
          </button>
        </div>
      )}
    </nav>
  );
};
