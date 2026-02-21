import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import Layout from './components/Layout';
import RoleGuard from './components/RoleGuard';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Vehicles from './pages/Vehicles';
import Drivers from './pages/Drivers';
import Trips from './pages/Trips';
import Maintenance from './pages/Maintenance';
import FuelLogs from './pages/FuelLogs';
import Reports from './pages/Reports';
import LiveMap from './pages/LiveMap';
import DriverProfile from './pages/DriverProfile';
import AIPredictive from './pages/AIPredictive';
import AIFinancial from './pages/AIFinancial';
import AIDriverPerformance from './pages/AIDriverPerformance';

function ProtectedRoute({ children }) {
    const { user, loading } = useAuth();
    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;
    return user ? children : <Navigate to="/login" />;
}

export default function App() {
    const { user } = useAuth();

    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to="/" /> : <Login />} />
            <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
                <Route index element={<Dashboard />} />
                <Route path="vehicles" element={<Vehicles />} />
                <Route path="drivers" element={<Drivers />} />
                <Route path="live-map" element={<LiveMap />} />
                <Route path="trips" element={<Trips />} />
                <Route path="maintenance" element={<Maintenance />} />
                <Route path="fuel-logs" element={<FuelLogs />} />
                <Route path="reports" element={<Reports />} />
                <Route path="profile" element={<DriverProfile />} />
                <Route path="ai/predictive" element={<RoleGuard allowedRoles={['MANAGER', 'ANALYST']}><AIPredictive /></RoleGuard>} />
                <Route path="ai/financial" element={<RoleGuard allowedRoles={['MANAGER', 'ANALYST']}><AIFinancial /></RoleGuard>} />
                <Route path="ai/driver-performance" element={<RoleGuard allowedRoles={['MANAGER', 'ANALYST']}><AIDriverPerformance /></RoleGuard>} />
            </Route>
            <Route path="*" element={<Navigate to="/" />} />
        </Routes>
    );
}
