import { useState, useEffect } from 'react';
import { aiAPI } from '../api';
import { Wrench, Package, DollarSign, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';

const formatINR = (val) => {
    const num = Number(val);
    if (isNaN(num)) return val;
    return '₹' + num.toLocaleString('en-IN');
};

export default function AIPredictive() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const res = await aiAPI.predictive();
                setData(res.data?.data || {});
            } catch (err) {
                toast.error('Failed to load predictive analytics');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;
    if (!data) return <div className="empty-state"><h3>No data available</h3></div>;

    const maintenance = data.maintenanceForecast30Days || [];
    const demand = data.demandForecastNextWeek || {};
    const revenue = data.revenueProjection || {};
    const driverRisk = data.driverRiskProbability || [];

    return (
        <div>
            <p className="page-description" style={{ marginBottom: 20 }}>
                Future-oriented admin view: maintenance forecast, demand forecast, revenue projection, and driver risk.
            </p>

            {/* Summary cards */}
            <div className="kpi-grid" style={{ marginBottom: 24 }}>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Maintenance due (30 days)</span>
                        <div className="kpi-icon" style={{ background: 'var(--amber-bg)' }}>
                            <Wrench size={18} color="var(--amber)" />
                        </div>
                    </div>
                    <div className="kpi-value">{maintenance.length} vehicles</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Predicted trips (next week)</span>
                        <div className="kpi-icon" style={{ background: 'var(--blue-bg)' }}>
                            <Package size={18} color="var(--blue)" />
                        </div>
                    </div>
                    <div className="kpi-value">{demand.predictedTripsNextWeek ?? 0}</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">Revenue projection (30 days)</span>
                        <div className="kpi-icon" style={{ background: 'var(--green-bg)' }}>
                            <DollarSign size={18} color="var(--green)" />
                        </div>
                    </div>
                    <div className="kpi-value">{formatINR(revenue.projectedRevenueNext30Days)}</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-header">
                        <span className="kpi-label">High-risk drivers</span>
                        <div className="kpi-icon" style={{ background: 'var(--red-bg)' }}>
                            <AlertTriangle size={18} color="var(--red)" />
                        </div>
                    </div>
                    <div className="kpi-value">
                        {driverRisk.filter(d => d.riskLevel === 'HIGH').length}
                    </div>
                </div>
            </div>

            {/* Maintenance forecast table */}
            <div className="chart-container" style={{ marginBottom: 24 }}>
                <h3>Maintenance forecast (next 30 days)</h3>
                <div className="data-table-wrapper">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Vehicle</th>
                                <th>Last service</th>
                                <th>Predicted due</th>
                                <th>Days until due</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {maintenance.length === 0 ? (
                                <tr><td colSpan={5} className="empty-state"><p>No maintenance due in next 30 days</p></td></tr>
                            ) : (
                                maintenance.map((row, i) => (
                                    <tr key={i}>
                                        <td><strong>{row.vehicleName}</strong></td>
                                        <td>{row.lastServiceDate}</td>
                                        <td>{row.predictedDueDate}</td>
                                        <td>{row.daysUntilDue}</td>
                                        <td>
                                            {row.overdue ? (
                                                <span style={{ color: 'var(--red)', fontWeight: 600 }}>Overdue</span>
                                            ) : (
                                                <span style={{ color: 'var(--amber)' }}>Due soon</span>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Demand & Revenue */}
            <div className="grid-2">
                <div className="chart-container">
                    <h3>Demand forecast (next week)</h3>
                    <ul style={{ listStyle: 'none', padding: 0 }}>
                        <li><strong>Predicted trips:</strong> {demand.predictedTripsNextWeek ?? 0}</li>
                        <li><strong>Basis:</strong> {demand.basis ?? '—'}</li>
                        <li><strong>Recent completed (14d):</strong> {demand.recentCompletedTrips ?? 0}</li>
                        <li><strong>Avg trips/day:</strong> {Number(demand.averageTripsPerDay ?? 0).toFixed(2)}</li>
                    </ul>
                </div>
                <div className="chart-container">
                    <h3>Revenue projection (next 30 days)</h3>
                    <ul style={{ listStyle: 'none', padding: 0 }}>
                        <li><strong>Projected:</strong> {formatINR(revenue.projectedRevenueNext30Days)}</li>
                        <li><strong>Recent 30-day revenue:</strong> {formatINR(revenue.recent30DayRevenue)}</li>
                        <li><strong>Avg revenue/day:</strong> {formatINR(revenue.averageRevenuePerDay)}</li>
                    </ul>
                </div>
            </div>

            {/* Driver risk probability */}
            <div className="chart-container" style={{ marginTop: 24 }}>
                <h3>Driver risk probability</h3>
                <div className="data-table-wrapper">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Driver</th>
                                <th>Safety score</th>
                                <th>Trip completion rate</th>
                                <th>License expiry</th>
                                <th>Risk probability</th>
                                <th>Risk level</th>
                            </tr>
                        </thead>
                        <tbody>
                            {driverRisk.length === 0 ? (
                                <tr><td colSpan={6} className="empty-state"><p>No driver data</p></td></tr>
                            ) : (
                                driverRisk.map((row, i) => (
                                    <tr key={i}>
                                        <td><strong>{row.driverName}</strong></td>
                                        <td>{row.safetyScore}</td>
                                        <td>{Number(row.tripCompletionRate).toFixed(1)}%</td>
                                        <td>{row.licenseExpiry ?? '—'}</td>
                                        <td>{Number(row.riskProbability).toFixed(1)}%</td>
                                        <td>
                                            <span className={`risk-badge risk-${(row.riskLevel || '').toLowerCase()}`}>
                                                {row.riskLevel}
                                            </span>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
