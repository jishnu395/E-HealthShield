import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { UserRole } from '../../types/auth';

interface RoleRouteProps {
  allowedRole: UserRole;
}

export const RoleRoute: React.FC<RoleRouteProps> = ({ allowedRole }) => {
  const { role } = useAuth();

  if (role !== allowedRole) {
    // Redirect to the user's appropriate workspace if trying to access unauthorized role route
    const fallbackPath = role === 'DOCTOR' ? '/doctor' : '/patient';
    return <Navigate to={fallbackPath} replace />;
  }

  return <Outlet />;
};
