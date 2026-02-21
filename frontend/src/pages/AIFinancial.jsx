import { useState, useEffect } from 'react';
import { aiAPI } from '../api';
import { Wallet, Fuel, Route, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';

const formatINR = (val) => {
    const num = Number(val);
    if (isNaN(num)) return val;
    return '₹' + num.toLocaleString('en-IN');
};

const tabs = [
    { key: 'cost', label: 'Cost per trip' },
    { key: 'fuel', label: 'Fuel efficiency per vehicle' },
    { key: 'profit', label: 'Profit per route' },
    { key: 'fraud', label: 'Fraud risk (fuel anomalies)' },
];

export default function AIFinancial() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('cost');

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const res = await aiAPI.financial();
                setData(res.data?.data || {});
            } catch (err) {
                toast.error('Failed to load financial intelligence');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;
    if (!data) return <div className="empty-state"><h3>No data available</h3></div>;

    const costPerTrip = data.costPerTrip || [];
    const fuelEfficiency = data.fuelEfficiencyPerVehicle || [];
    const profitPerRoute = data.profitPerRoute || [];
    const fraudRisk = data.fraudRiskScore || [];
    const highFraud = fraudRisk.filter(r => (r.riskLevel || '').toUpperCase() === 'HIGH').length;

    return (
        <div>
            <p className="page-description" style={{ marginBottom: 20 }}>
                Admin-focused financial metrics: cost per trip, fuel efficiency, profit per route, fraud risk.
            </p>

            <div className="kpi-grid" style={{ marginBottom: 24 }}>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Trips (cost data)</span>
                        <div className="kpi-icon" style={{ background: 'var(--blue-bg)' }}>
                            <Wallet size={18} color="var(--blue)" />
                        </div>
                    </div>
                    <div className="kpi-value">{costPerTrip.length}</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Vehicles (fuel efficiency)</span>
                        <div className="kpi-icon" style={{ background: 'var(--green-bg)' }}>
                            <Fuel size={18} color="var(--green)" />
                        </div>
                    </div>
                    <div className="kpi-value">{fuelEfficiency.length}</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Routes (profit)</span>
                        <div className="kpi-icon" style={{ background: 'var(--teal-light)' }}>
                            <Route size={18} color="var(--teal)" />
                        </div>
                    </div>
                    <div className="kpi-value">{profitPerRoute.length}</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">High fraud risk</span>
                        <div className="kpi-icon" style={{ background: 'var(--red-bg)' }}>
                            <AlertTriangle size={18} color="var(--red)" />
                        </div>
                    </div>
                    <div className="kpi-value">{highFraud}</div>
                </div>
            </div>

            <div className="tab-nav" style={{ marginBottom: 20 }}>
                {tabs.map(t => (
                    <button
                        key={t.key}
                        className={`tab-btn ${activeTab === t.key ? 'active' : ''}`}
                        onClick={() => setActiveTab(t.key)}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            {activeTab === 'cost' && (
                <div className="chart-container">
                    <h3>Cost per trip</h3>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Trip</th>
                                    <th>Origin → Destination</th>
                                    <th>Vehicle</th>
                                    <th>Revenue</th>
                                    <th>Cost per trip</th>
                                </tr>
                            </thead>
                            <tbody>
                                {costPerTrip.length === 0 ? (
                                    <tr><td colSpan={5} className="empty-state"><p>No trip data</p></td></tr>
                                ) : (
                                    costPerTrip.slice(0, 50).map((row, i) => (
                                        <tr key={i}>
                                            <td>#{row.tripId}</td>
                                            <td>{row.origin} → {row.destination}</td>
                                            <td>{row.vehicleName}</td>
                                            <td style={{ color: 'var(--green)' }}>{formatINR(row.revenue)}</td>
                                            <td style={{ fontWeight: 600 }}>{formatINR(row.costPerTrip)}</td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {activeTab === 'fuel' && (
                <div className="chart-container">
                    <h3>Fuel efficiency per vehicle</h3>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Vehicle</th>
                                    <th>License</th>
                                    <th>Total km</th>
                                    <th>Total liters</th>
                                    <th>km/L</th>
                                </tr>
                            </thead>
                            <tbody>
                                {fuelEfficiency.map((row, i) => (
                                    <tr key={i}>
                                        <td><strong>{row.vehicleName}</strong></td>
                                        <td style={{ fontFamily: 'monospace' }}>{row.licensePlate}</td>
                                        <td>{Number(row.totalKm).toLocaleString()}</td>
                                        <td>{Number(row.totalLiters).toLocaleString()}</td>
                                        <td style={{ fontWeight: 600, color: 'var(--green)' }}>
                                            {Number(row.kmPerLiter).toFixed(2)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {activeTab === 'profit' && (
                <div className="chart-container">
                    <h3>Profit per route</h3>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Route</th>
                                    <th>Trip count</th>
                                    <th>Total revenue</th>
                                    <th>Total cost</th>
                                    <th>Total profit</th>
                                </tr>
                            </thead>
                            <tbody>
                                {profitPerRoute.map((row, i) => (
                                    <tr key={i}>
                                        <td><strong>{row.route}</strong></td>
                                        <td>{row.tripCount}</td>
                                        <td style={{ color: 'var(--green)' }}>{formatINR(row.totalRevenue)}</td>
                                        <td>{formatINR(row.totalCost)}</td>
                                        <td style={{
                                            color: Number(row.totalProfit) >= 0 ? 'var(--green)' : 'var(--red)',
                                            fontWeight: 600
                                        }}>
                                            {formatINR(row.totalProfit)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {activeTab === 'fraud' && (
                <div className="chart-container">
                    <h3>Fraud risk score (fuel anomalies)</h3>
                    <div className="data-table-wrapper">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Vehicle</th>
                                    <th>License</th>
                                    <th>Avg cost/liter</th>
                                    <th>Fraud risk score</th>
                                    <th>Risk level</th>
                                </tr>
                            </thead>
                            <tbody>
                                {fraudRisk.map((row, i) => (
                                    <tr key={i}>
                                        <td><strong>{row.vehicleName}</strong></td>
                                        <td style={{ fontFamily: 'monospace' }}>{row.licensePlate}</td>
                                        <td>{formatINR(row.avgCostPerLiter)}</td>
                                        <td>{Number(row.fraudRiskScore).toFixed(2)}</td>
                                        <td>
                                            <span className={`risk-badge risk-${(row.riskLevel || '').toLowerCase()}`}>
                                                {row.riskLevel}
                                            </span>
                                        </td>
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
