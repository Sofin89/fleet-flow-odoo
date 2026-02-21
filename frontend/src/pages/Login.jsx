import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Lock, Mail, Truck, MapPin, BarChart3 } from 'lucide-react';

export default function Login() {
    const { login } = useAuth();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await login(email, password);
        } catch (err) {
            setError(err.response?.data?.message || 'Invalid credentials');
        } finally {
            setLoading(false);
        }
    };

    const quickLogin = (acc) => {
        setEmail(acc.email);
        setPassword('password123');
    };

    return (
        <div className="login-page">
            <div className="login-left">
                <div style={{ marginBottom: 40 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 40 }}>
                        <div style={{ width: 40, height: 40, background: '#fff', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#0c2340', fontWeight: 800, fontSize: 16 }}>FF</div>
                        <span style={{ fontSize: '1.3rem', fontWeight: 700 }}>FleetFlow India</span>
                    </div>
                    <h2>Fleet & logistics management for Indian operations.</h2>
                    <p style={{ marginTop: 16 }}>Track vehicles across Gujarat, dispatch trips, monitor driver safety, and analyze costs — all from one platform built for Indian fleet operators.</p>
                </div>
                <div style={{ display: 'flex', gap: 32, marginTop: 'auto' }}>
                    {[
                        { icon: Truck, label: 'Vehicle Tracking', desc: 'Real-time GPS fleet status' },
                        { icon: MapPin, label: 'Live Map', desc: 'Ahmedabad to pan-India' },
                        { icon: BarChart3, label: 'Analytics', desc: '₹ cost & ROI reports' },
                    ].map(({ icon: Icon, label, desc }) => (
                        <div key={label} style={{ flex: 1 }}>
                            <Icon size={22} style={{ marginBottom: 8, opacity: 0.7 }} />
                            <div style={{ fontWeight: 600, fontSize: '0.9rem', marginBottom: 2 }}>{label}</div>
                            <div style={{ fontSize: '0.78rem', opacity: 0.55 }}>{desc}</div>
                        </div>
                    ))}
                </div>
            </div>

            <div className="login-right">
                <div className="login-card">
                    <div className="login-logo">
                        <div className="logo-text">Sign in</div>
                        <div className="subtitle">Enter your credentials to access the dashboard</div>
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label>Email</label>
                            <input className="form-input" type="email" placeholder="name@company.com" value={email}
                                onChange={(e) => setEmail(e.target.value)} required />
                        </div>
                        <div className="form-group">
                            <label>Password</label>
                            <input className="form-input" type="password" placeholder="••••••••" value={password}
                                onChange={(e) => setPassword(e.target.value)} required />
                        </div>
                        <button className="btn btn-primary" type="submit" disabled={loading}
                            style={{ width: '100%', marginTop: 6, padding: '11px 20px' }}>
                            {loading ? 'Signing in…' : 'Sign In'}
                        </button>
                    </form>

                    <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
                        <p style={{ fontSize: '0.75rem', color: 'var(--text-400)', marginBottom: 8, textAlign: 'center' }}>Quick Login — Select Role</p>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6 }}>
                            {[
                                { email: 'manager@fleetflow.com', label: '🏢 Fleet Manager', desc: 'Full access' },
                                { email: 'dispatcher@fleetflow.com', label: '📋 Dispatcher', desc: 'Trips & fleet' },
                                { email: 'safety@fleetflow.com', label: '🛡 Safety Officer', desc: 'Drivers & safety' },
                                { email: 'analyst@fleetflow.com', label: '📊 Analyst', desc: 'Reports & data' },
                            ].map(acc => (
                                <button key={acc.email} className="btn btn-secondary btn-sm"
                                    onClick={() => quickLogin(acc)} type="button"
                                    style={{ textAlign: 'left', padding: '8px 10px' }}>
                                    <div style={{ fontWeight: 600, fontSize: '0.8rem' }}>{acc.label}</div>
                                    <div style={{ fontSize: '0.68rem', opacity: 0.6, marginTop: 1 }}>{acc.desc}</div>
                                </button>
                            ))}
                        </div>
                        <button className="btn btn-primary btn-sm"
                            onClick={() => quickLogin({ email: 'driver@fleetflow.com' })}
                            type="button"
                            style={{ width: '100%', marginTop: 6, padding: '10px 14px' }}>
                            <div style={{ fontWeight: 600, fontSize: '0.82rem' }}>🚛 Driver Login</div>
                            <div style={{ fontSize: '0.68rem', opacity: 0.7, marginTop: 1 }}>Map & trip view only</div>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
