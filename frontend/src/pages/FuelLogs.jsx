import { useState, useEffect } from 'react';
import { fuelLogAPI, vehicleAPI } from '../api';
import { Plus, X, Fuel } from 'lucide-react';
import toast from 'react-hot-toast';

export default function FuelLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [showModal, setShowModal] = useState(false);
    const [vehicles, setVehicles] = useState([]);
    const [form, setForm] = useState({
        vehicleId: '', liters: '', cost: '', logDate: ''
    });

    useEffect(() => { loadLogs(); }, [page]);

    const loadLogs = async () => {
        try {
            setLoading(true);
            const res = await fuelLogAPI.getAll({ page, size: 20 });
            setLogs(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            toast.error('Failed to load fuel logs');
        } finally { setLoading(false); }
    };

    const openCreate = async () => {
        try {
            const vRes = await vehicleAPI.getAll({ size: 100 });
            setVehicles(vRes.data.data.content || []);
            setShowModal(true);
        } catch (err) {
            toast.error('Failed to load vehicles');
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            await fuelLogAPI.create({
                vehicleId: parseInt(form.vehicleId),
                liters: parseFloat(form.liters),
                cost: parseFloat(form.cost),
                logDate: form.logDate,
            });
            toast.success('Fuel log created');
            setShowModal(false);
            setForm({ vehicleId: '', liters: '', cost: '', logDate: '' });
            loadLogs();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to create fuel log');
        }
    };

    if (loading && logs.length === 0) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="toolbar-filters">
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        <Fuel size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                        Track fuel consumption and costs per vehicle
                    </span>
                </div>
                <button className="btn btn-primary" onClick={openCreate}><Plus size={16} /> Log Fuel</button>
            </div>

            <div className="data-table-wrapper">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Vehicle</th><th>Liters</th><th>Cost</th><th>Cost/Liter</th><th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        {logs.length === 0 ? (
                            <tr><td colSpan="5" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No fuel logs</td></tr>
                        ) : logs.map(f => (
                            <tr key={f.id}>
                                <td><strong>{f.vehicleName}</strong><br /><span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{f.vehicleLicensePlate}</span></td>
                                <td>{Number(f.liters).toFixed(1)} L</td>
                                <td style={{ fontWeight: 600 }}>${Number(f.cost).toLocaleString()}</td>
                                <td style={{ color: 'var(--text-muted)' }}>${(Number(f.cost) / Number(f.liters)).toFixed(2)}/L</td>
                                <td>{f.logDate}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {totalPages > 1 && (
                    <div className="pagination">
                        <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</button>
                        <span className="page-info">Page {page + 1} of {totalPages}</span>
                        <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>Next</button>
                    </div>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Log Fuel</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={handleCreate}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Vehicle *</label>
                                    <select className="form-select" value={form.vehicleId} onChange={e => setForm({ ...form, vehicleId: e.target.value })} required>
                                        <option value="">Select Vehicle</option>
                                        {vehicles.map(v => (
                                            <option key={v.id} value={v.id}>{v.name} ({v.licensePlate})</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Liters *</label>
                                        <input className="form-input" type="number" step="0.01" value={form.liters} onChange={e => setForm({ ...form, liters: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label>Cost ($) *</label>
                                        <input className="form-input" type="number" step="0.01" value={form.cost} onChange={e => setForm({ ...form, cost: e.target.value })} required />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label>Date *</label>
                                    <input className="form-input" type="date" value={form.logDate} onChange={e => setForm({ ...form, logDate: e.target.value })} required />
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">Create Log</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
