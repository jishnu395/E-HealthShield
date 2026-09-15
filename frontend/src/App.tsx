import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { ProtectedRoute } from './components/routing/ProtectedRoute';
import { RoleRoute } from './components/routing/RoleRoute';
import { AppLayout } from './layouts/AppLayout';
import { PatientLayout } from './layouts/PatientLayout';
import { DoctorLayout } from './layouts/DoctorLayout';

import { PatientDashboardPage } from './pages/patient/PatientDashboardPage';
import { PatientRecordsPage } from './pages/patient/PatientRecordsPage';
import { PatientAccessPage } from './pages/patient/PatientAccessPage';
import { PatientAuditPage } from './pages/patient/PatientAuditPage';

import { DoctorDashboardPage } from './pages/doctor/DoctorDashboardPage';
import { DoctorUploadPage } from './pages/doctor/DoctorUploadPage';
import { DoctorSearchPage } from './pages/doctor/DoctorSearchPage';
import { DoctorRecordsPage } from './pages/doctor/DoctorRecordsPage';

export const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      {/* Protected Parent Layout */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          
          {/* Patient Routes */}
          <Route element={<RoleRoute allowedRole="PATIENT" />}>
            <Route element={<PatientLayout />}>
              <Route path="/patient" element={<PatientDashboardPage />} />
              <Route path="/patient/records" element={<PatientRecordsPage />} />
              <Route path="/patient/access" element={<PatientAccessPage />} />
              <Route path="/patient/audit" element={<PatientAuditPage />} />
            </Route>
          </Route>

          {/* Doctor Routes */}
          <Route element={<RoleRoute allowedRole="DOCTOR" />}>
            <Route element={<DoctorLayout />}>
              <Route path="/doctor" element={<DoctorDashboardPage />} />
              <Route path="/doctor/upload" element={<DoctorUploadPage />} />
              <Route path="/doctor/search" element={<DoctorSearchPage />} />
              <Route path="/doctor/records" element={<DoctorRecordsPage />} />
            </Route>
          </Route>

        </Route>
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default App;
