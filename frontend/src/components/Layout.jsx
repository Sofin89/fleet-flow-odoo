import { Fragment } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LogOut } from 'lucide-react';
import { getNavigationForRole } from '../config/roleNavigation';

const pageTitles = {
    '/': 'Dashboard',
    '/vehicles': 'Vehicle Registry',
    '/drivers': 'Driver Profiles',
    '/live-map': 'Live Fleet Map',
    '/trips': 'Trip Dispatcher',
    '/maintenance': 'Maintenance Logs',
    '/fuel-logs': 'Fuel & Expenses',
    '/reports': 'Analytics & Reports',
    '/ai/predictive': 'Predictive Analytics',
    '/ai/financial': 'Financial Intelligence',
    '/ai/driver-performance': 'Driver Performance',
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

    // Get navigation items for user's role using ROLE_NAVIGATION configuration
    const menuItems = getNavigationForRole(userRole);

    return (
        <div className="app-layout">
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <div className="logo-icon">FF</div>
                    <h1>FleetFlow</h1>
                </div>
                <nav className="sidebar-nav">
                    {menuItems.map((item, idx) => {
                        if (item.groupLabel && item.items) {
                            return (
                                <Fragment key={`ai-${idx}`}>
                                    <div className="nav-group-label">
                                        <item.icon size={14} /> <span>{item.groupLabel}</span>
                                    </div>
                                    {item.items.map(({ path, label, icon: Icon }) => (
                                        <NavLink
                                            key={path}
                                            to={path}
                                            className={({ isActive }) => `nav-link nav-link-sub ${isActive ? 'active' : ''}`}
                                        >
                                            <Icon size={18} /> <span>{label}</span>
                                        </NavLink>
                                    ))}
                                </Fragment>
                            );
                        }
                        const { path, label, icon: Icon } = item;
                        return (
                            <NavLink
                                key={path}
                                to={path}
                                end={path === '/'}
                                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                            >
                                <Icon /> <span>{label}</span>
                            </NavLink>
                        );
                    })}
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
