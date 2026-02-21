import { useState, useEffect } from 'react';
import { aiAPI } from '../api';
import { Award, AlertTriangle, Trophy, Users } from 'lucide-react';
import toast from 'react-hot-toast';

export default function AIDriverPerformance() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const res = await aiAPI.driverPerformance();
                setData(res.data?.data || {});
            } catch (err) {
                toast.error('Failed to load driver performance');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;
    if (!data) return <div className="empty-state"><h3>No data available</h3></div>;

    const leaderboard = data.leaderboard || [];
    const topPerformerId = data.topPerformerId;
    const highRiskDriverIds = data.highRiskDriverIds || [];

    return (
        <div>
            <p className="page-description" style={{ marginBottom: 20 }}>
                Driver ranking: Safety score, efficiency, on-time %, risk level. Top performer badge and high-risk alerts for HR decisions.
            </p>

            {/* Alerts */}
            {highRiskDriverIds.length > 0 && (
                <div className="alert-box" style={{
                    background: 'var(--red-bg)',
                    border: '1px solid var(--red)',
                    borderRadius: 8,
                    padding: 12,
                    marginBottom: 20,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10
                }}>
                    <AlertTriangle size={20} color="var(--red)" />
                    <div>
                        <strong>High-risk alert</strong>: {highRiskDriverIds.length} driver(s) flagged for review.
                        {leaderboard.filter(d => highRiskDriverIds.includes(d.driverId)).map(d => d.driverName).join(', ')}
                    </div>
                </div>
            )}

            {topPerformerId && (
                <div className="alert-box" style={{
                    background: 'var(--green-bg)',
                    border: '1px solid var(--green)',
                    borderRadius: 8,
                    padding: 12,
                    marginBottom: 20,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10
                }}>
                    <Trophy size={20} color="var(--green)" />
                    <div>
                        <strong>Top performer</strong>: {leaderboard.find(d => d.driverId === topPerformerId)?.driverName ?? '—'}
                        <span style={{ marginLeft: 6 }}>🏆</span>
                    </div>
                </div>
            )}

            <div className="chart-container">
                <h3>Leaderboard — Driver | Safety score | Efficiency | On-time % | Risk level</h3>
                <div className="data-table-wrapper">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Driver</th>
                                <th>Safety score</th>
                                <th>Efficiency</th>
                                <th>On-time %</th>
                                <th>Risk level</th>
                                <th>Badge</th>
                            </tr>
                        </thead>
                        <tbody>
                            {leaderboard.length === 0 ? (
                                <tr><td colSpan={7} className="empty-state"><p>No driver data</p></td></tr>
                            ) : (
                                leaderboard.map((row, i) => (
                                    <tr
                                        key={row.driverId}
                                        style={{
                                            background: highRiskDriverIds.includes(row.driverId)
                                                ? 'rgba(239, 68, 68, 0.06)'
                                                : undefined
                                        }}
                                    >
                                        <td>{i + 1}</td>
                                        <td><strong>{row.driverName}</strong></td>
                                        <td>{row.safetyScore}</td>
                                        <td>{Number(row.efficiency).toFixed(2)}</td>
                                        <td>{Number(row.onTimePercent).toFixed(1)}%</td>
                                        <td>
                                            <span className={`risk-badge risk-${(row.riskLevel || '').toLowerCase()}`}>
                                                {row.riskLevel}
                                            </span>
                                        </td>
                                        <td>
                                            {row.driverId === topPerformerId && (
                                                <span title="Top performer">🏆</span>
                                            )}
                                            {highRiskDriverIds.includes(row.driverId) && (
                                                <span title="High risk" style={{ color: 'var(--red)', marginLeft: 4 }}>⚠</span>
                                            )}
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
