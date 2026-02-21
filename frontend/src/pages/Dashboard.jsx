import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { dashboardAPI } from '../api';
import { Truck, AlertTriangle, Activity, Package, Users, CheckCircle, IndianRupee, TrendingUp } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import toast from 'react-hot-toast';
import DriverDashboard from './DriverDashboard';

const kpiConfig = [
    { key: 'totalVehicles', label: 'Total Vehicles', icon: Truck, color: '#2980b9', bg: '#d6eaf8' },
    { key: 'totalDrivers', label: 'Total Drivers', icon: Users, color: '#17a589', bg: '#d1f2eb' },
    { key: 'activeFleet', label: 'On Trip', icon: Activity, color: '#2e86c1', bg: '#d6eaf8' },
    { key: 'maintenanceAlerts', label: 'In Maintenance', icon: AlertTriangle, color: '#d68910', bg: '#fef5e7' },
    { key: 'completedTrips', label: 'Completed Trips', icon: CheckCircle, color: '#27ae60', bg: '#eafaf1' },
    { key: 'pendingCargo', label: 'Pending Cargo', icon: Package, color: '#6c3483', bg: '#f5eef8' },
    { key: 'totalRevenue', label: 'Revenue', icon: IndianRupee, color: '#27ae60', bg: '#eafaf1', prefix: '₹' },
    { key: 'utilizationRate', label: 'Utilization', icon: TrendingUp, color: '#2980b9', bg: '#d6eaf8', suffix: '%' },
];

const PIE_COLORS = ['#2980b9', '#d68910', '#27ae60', '#7f8c8d'];
const tooltipStyle = { background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, color: '#374151', fontSize: '0.82rem' };

const formatINR = (val) => {
    const num = Number(val);
    if (isNaN(num)) return val;
    return num.toLocaleString('en-IN');
};

export default function Dashboard() {
    const { user } = useAuth();

    // If user is a driver, show driver-specific dashboard
    if (user?.role === 'DRIVER') {
        return <DriverDashboard />;
    }

    // Otherwise show the regular dashboard for managers, dispatchers, etc.
    const [kpis, setKpis] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => { loadKPIs(); }, []);

    const loadKPIs = async () => {
        try {
            const res = await dashboardAPI.getKPIs();
            setKpis(res.data.data);
        } catch (err) {
            toast.error('Failed to load dashboard data');
        } finally { setLoading(false); }
    };

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;
    if (!kpis) return <div className="empty-state"><h3>Failed to load dashboard</h3></div>;

    const pieData = [
        { name: 'On Trip', value: kpis.activeFleet || 0 },
        { name: 'In Shop', value: kpis.maintenanceAlerts || 0 },
        { name: 'Available', value: Math.max(0, (kpis.totalVehicles || 0) - (kpis.activeFleet || 0) - (kpis.maintenanceAlerts || 0)) },
    ].filter(d => d.value > 0);

    const barData = [
        { name: 'Revenue', value: Number(kpis.totalRevenue) || 0 },
        { name: 'Expenses', value: Number(kpis.totalExpenses) || 0 },
        { name: 'Net Profit', value: (Number(kpis.totalRevenue) || 0) - (Number(kpis.totalExpenses) || 0) },
    ];

    return (
        <div>
            <div className="kpi-grid">
                {kpiConfig.map(({ key, label, icon: Icon, color, bg, suffix, prefix }) => (
                    <div className="kpi-card" key={key}>
                        <div className="kpi-header">
                            <span className="kpi-label">{label}</span>
                            <div className="kpi-icon" style={{ background: bg }}>
                                <Icon size={18} color={color} />
                            </div>
                        </div>
                        <div className="kpi-value">
                            {prefix}{key === 'totalRevenue' || key === 'totalExpenses'
                                ? formatINR(kpis[key])
                                : typeof kpis[key] === 'number'
                                    ? (key === 'utilizationRate' ? kpis[key].toFixed(1) : kpis[key].toLocaleString('en-IN'))
                                    : kpis[key]}{suffix}
                        </div>
                    </div>
                ))}
            </div>

            <div className="grid-2">
                <div className="chart-container">
                    <h3>Fleet Status</h3>
                    {pieData.length > 0 ? (
                        <ResponsiveContainer width="100%" height={260}>
                            <PieChart>
                                <Pie data={pieData} cx="50%" cy="50%" innerRadius={55} outerRadius={90}
                                    paddingAngle={4} dataKey="value">
                                    {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                                </Pie>
                                <Legend verticalAlign="bottom" iconType="circle" wrapperStyle={{ fontSize: '0.78rem', color: '#6b7280' }} />
                                <Tooltip contentStyle={tooltipStyle} />
                            </PieChart>
                        </ResponsiveContainer>
                    ) : (
                        <div className="empty-state"><p>No fleet data yet</p></div>
                    )}
                </div>

                <div className="chart-container">
                    <h3>Financial Summary (₹)</h3>
                    <ResponsiveContainer width="100%" height={260}>
                        <BarChart data={barData}>
                            <XAxis dataKey="name" stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false} />
                            <YAxis stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false}
                                tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}K`} />
                            <Tooltip contentStyle={tooltipStyle}
                                formatter={(v) => [`₹${formatINR(v)}`, '']} />
                            <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                                {barData.map((entry, i) => (
                                    <Cell key={i} fill={entry.value >= 0 ? ['#2980b9', '#d68910', '#27ae60'][i] : '#c0392b'} />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>
        </div>
    );
}
