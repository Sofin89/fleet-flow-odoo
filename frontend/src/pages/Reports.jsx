import { useState, useEffect } from 'react';
import { reportAPI } from '../api';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line, CartesianGrid, Legend } from 'recharts';
import { Download } from 'lucide-react';
import toast from 'react-hot-toast';

const tabs = [
    { key: 'fuel', label: 'Fuel Efficiency' },
    { key: 'roi', label: 'Vehicle ROI' },
    { key: 'costs', label: 'Operational Costs' },
];

const tooltipStyle = { background: '#1e293b', border: '1px solid rgba(99,102,241,0.3)', borderRadius: 8, color: '#f1f5f9' };

export default function Reports() {
    const [activeTab, setActiveTab] = useState('fuel');
    const [fuelData, setFuelData] = useState([]);
    const [roiData, setRoiData] = useState([]);
    const [costData, setCostData] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => { loadReports(); }, []);

    const loadReports = async () => {
        try {
            setLoading(true);
            const [fuelRes, roiRes, costRes] = await Promise.all([
                reportAPI.fuelEfficiency(),
                reportAPI.vehicleROI(),
                reportAPI.operationalCosts(),
            ]);
            setFuelData(fuelRes.data.data || []);
            setRoiData(roiRes.data.data || []);
            setCostData(costRes.data.data || []);
        } catch (err) {
            toast.error('Failed to load reports');
        } finally { setLoading(false); }
    };

    const exportCSV = (data, filename) => {
        if (!data.length) return;
        const headers = Object.keys(data[0]).join(',');
        const rows = data.map(row => Object.values(row).join(',')).join('\n');
        const csv = headers + '\n' + rows;
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `${filename}.csv`;
        a.click(); URL.revokeObjectURL(url);
        toast.success(`Exported ${filename}.csv`);
    };

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="tab-nav">
                    {tabs.map(t => (
                        <button key={t.key}
                            className={`tab-btn ${activeTab === t.key ? 'active' : ''}`}
                            onClick={() => setActiveTab(t.key)}>
                            {t.label}
                        </button>
                    ))}
                </div>
                <button className="btn btn-secondary" onClick={() => {
                    const dataMap = { fuel: fuelData, roi: roiData, costs: costData };
                    exportCSV(dataMap[activeTab], `fleetflow_${activeTab}_report`);
                }}>
                    <Download size={16} /> Export CSV
                </button>
            </div>

            {/* Fuel Efficiency Tab */}
            {activeTab === 'fuel' && (
                <div>
                    <div className="chart-container" style={{ marginBottom: 24 }}>
                        <h3>Fuel Efficiency (km/L) by Vehicle</h3>
                        {fuelData.length > 0 ? (
                            <ResponsiveContainer width="100%" height={350}>
                                <BarChart data={fuelData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(99,102,241,0.1)" />
                                    <XAxis dataKey="vehicleName" stroke="#64748b" fontSize={12} />
                                    <YAxis stroke="#64748b" fontSize={12} />
                                    <Tooltip contentStyle={tooltipStyle} />
                                    <Bar dataKey="kmPerLiter" fill="#6366f1" radius={[6, 6, 0, 0]} name="km/L" />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : <div className="empty-state"><p>No fuel data available yet</p></div>}
                    </div>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead><tr><th>Vehicle</th><th>License Plate</th><th>Total KM</th><th>Total Liters</th><th>km/L</th></tr></thead>
                            <tbody>
                                {fuelData.map((r, i) => (
                                    <tr key={i}>
                                        <td>{r.vehicleName}</td>
                                        <td style={{ fontFamily: 'monospace' }}>{r.licensePlate}</td>
                                        <td>{Number(r.totalKm).toLocaleString()}</td>
                                        <td>{Number(r.totalLiters).toLocaleString()}</td>
                                        <td style={{ fontWeight: 600, color: '#6366f1' }}>{Number(r.kmPerLiter).toFixed(2)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Vehicle ROI Tab */}
            {activeTab === 'roi' && (
                <div>
                    <div className="chart-container" style={{ marginBottom: 24 }}>
                        <h3>Vehicle ROI (%)</h3>
                        {roiData.length > 0 ? (
                            <ResponsiveContainer width="100%" height={350}>
                                <BarChart data={roiData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(99,102,241,0.1)" />
                                    <XAxis dataKey="vehicleName" stroke="#64748b" fontSize={12} />
                                    <YAxis stroke="#64748b" fontSize={12} />
                                    <Tooltip contentStyle={tooltipStyle} />
                                    <Legend />
                                    <Bar dataKey="revenue" fill="#22c55e" name="Revenue" radius={[6, 6, 0, 0]} />
                                    <Bar dataKey="totalCost" fill="#ef4444" name="Total Cost" radius={[6, 6, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : <div className="empty-state"><p>No ROI data available yet</p></div>}
                    </div>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead><tr><th>Vehicle</th><th>Revenue</th><th>Maintenance</th><th>Fuel</th><th>Profit</th><th>ROI %</th></tr></thead>
                            <tbody>
                                {roiData.map((r, i) => (
                                    <tr key={i}>
                                        <td>{r.vehicleName}</td>
                                        <td style={{ color: '#22c55e' }}>${Number(r.revenue).toLocaleString()}</td>
                                        <td>${Number(r.maintenanceCost).toLocaleString()}</td>
                                        <td>${Number(r.fuelCost).toLocaleString()}</td>
                                        <td style={{ color: Number(r.profit) >= 0 ? '#22c55e' : '#ef4444', fontWeight: 600 }}>
                                            ${Number(r.profit).toLocaleString()}
                                        </td>
                                        <td style={{ color: Number(r.roiPercent) >= 0 ? '#22c55e' : '#ef4444', fontWeight: 700 }}>
                                            {Number(r.roiPercent).toFixed(2)}%
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Operational Costs Tab */}
            {activeTab === 'costs' && (
                <div>
                    <div className="chart-container" style={{ marginBottom: 24 }}>
                        <h3>Total Operational Cost by Vehicle</h3>
                        {costData.length > 0 ? (
                            <ResponsiveContainer width="100%" height={350}>
                                <BarChart data={costData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(99,102,241,0.1)" />
                                    <XAxis dataKey="vehicleName" stroke="#64748b" fontSize={12} />
                                    <YAxis stroke="#64748b" fontSize={12} />
                                    <Tooltip contentStyle={tooltipStyle} />
                                    <Legend />
                                    <Bar dataKey="maintenanceCost" fill="#f59e0b" name="Maintenance" stackId="cost" radius={[0, 0, 0, 0]} />
                                    <Bar dataKey="fuelCost" fill="#3b82f6" name="Fuel" stackId="cost" radius={[6, 6, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : <div className="empty-state"><p>No cost data available yet</p></div>}
                    </div>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead><tr><th>Vehicle</th><th>Maintenance</th><th>Fuel</th><th>Total Cost</th><th>Total KM</th><th>Cost/KM</th></tr></thead>
                            <tbody>
                                {costData.map((r, i) => (
                                    <tr key={i}>
                                        <td>{r.vehicleName}</td>
                                        <td>${Number(r.maintenanceCost).toLocaleString()}</td>
                                        <td>${Number(r.fuelCost).toLocaleString()}</td>
                                        <td style={{ fontWeight: 600 }}>${Number(r.totalOperationalCost).toLocaleString()}</td>
                                        <td>{Number(r.totalKm).toLocaleString()}</td>
                                        <td style={{ fontWeight: 600, color: '#8b5cf6' }}>${Number(r.costPerKm).toFixed(2)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
