import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
    LayoutDashboard, Truck, Users, Navigation, Wrench,
    Fuel, BarChart3, LogOut, MapPin
} from 'lucide-react';

// Navigation items with role-based access
const navItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard, section: 'overview', roles: ['MANAGER', 'ANALYST', 'DISPATCHER', 'SAFETY_OFFICER'] },
    { path: '/vehicles', label: 'Vehicles', icon: Truck, section: 'fleet', roles: ['MANAGER', 'DISPATCHER', 'SAFETY_OFFICER'] },
    { path: '/drivers', label: 'Drivers', icon: Users, section: 'fleet', roles: ['MANAGER', 'DISPATCHER', 'SAFETY_OFFICER'] },
    { path: '/live-map', label: 'Live Map', icon: MapPin, section: 'fleet', roles: ['MANAGER', 'DISPATCHER', 'DRIVER'] },
    { path: '/trips', label: 'Trips', icon: Navigation, section: 'ops', roles: ['MANAGER', 'DISPATCHER', 'DRIVER'] },
    { path: '/maintenance', label: 'Maintenance', icon: Wrench, section: 'ops', roles: ['MANAGER', 'SAFETY_OFFICER'] },
    { path: '/fuel-logs', label: 'Fuel & Expenses', icon: Fuel, section: 'ops', roles: ['MANAGER', 'ANALYST'] },
    { path: '/reports', label: 'Reports', icon: BarChart3, section: 'analytics', roles: ['MANAGER', 'ANALYST', 'SAFETY_OFFICER'] },
];

const sections = [
    { key: 'overview', label: 'Overview' },
    { key: 'fleet', label: 'Fleet' },
    { key: 'ops', label: 'Operations' },
    { key: 'analytics', label: 'Analytics' },
];

const pageTitles = {
    '/': 'Dashboard',
    '/vehicles': 'Vehicle Registry',
    '/drivers': 'Driver Profiles',
    '/live-map': 'Live Fleet Map',
    '/trips': 'Trip Dispatcher',
    '/maintenance': 'Maintenance Logs',
    '/fuel-logs': 'Fuel & Expenses',
    '/reports': 'Analytics & Reports',
};

const roleLabels = {
    MANAGER: 'Fleet Manager',
    DISPATCHER: 'Dispatcher',
    SAFETY_OFFICER: 'Safety Officer',
    ANALYST: 'Analyst',
    DRIVER: 'Driver',
};

export default function Layout() {
    const { user, logout } = useAuth();
    const location = useLocation();
    const title = pageTitles[location.pathname] || 'FleetFlow';
    const userRole = user?.role || 'MANAGER';

    // Filter nav items by user role
    const visibleItems = navItems.filter(n => n.roles.includes(userRole));
    // Only show sections that have visible items
    const visibleSections = sections.filter(sec =>
        visibleItems.some(n => n.section === sec.key)
    );

    return (
        <div className="app-layout">
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <div className="logo-icon">FF</div>
                    <h1>FleetFlow</h1>
                </div>
                <nav className="sidebar-nav">
                    {visibleSections.map(sec => (
                        <div key={sec.key}>
                            <div className="nav-section-label">{sec.label}</div>
                            {visibleItems.filter(n => n.section === sec.key).map(({ path, label, icon: Icon }) => (
                                <NavLink key={path} to={path} end={path === '/'} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                                    <Icon /> <span>{label}</span>
                                </NavLink>
                            ))}
                        </div>
                    ))}
                </nav>
                <div className="sidebar-footer">
                    <div className="user-info">
                        <div className="user-avatar">{user?.fullName?.charAt(0).toUpperCase()}</div>
                        <div className="user-details">
                            <div className="name">{user?.fullName}</div>
                            <div className="role">{roleLabels[userRole] || userRole}</div>
                        </div>
                        <button className="logout-btn" onClick={logout} title="Sign out"><LogOut size={16} /></button>
                    </div>
                </div>
            </aside>
            <div className="main-area">
                <header className="header">
                    <h2>{title}</h2>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-400)' }}>
                        {roleLabels[userRole]} · Ahmedabad Region
                    </div>
                </header>
                <main className="main-content">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
