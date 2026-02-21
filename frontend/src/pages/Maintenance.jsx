import { useState, useEffect } from 'react';
import { maintenanceAPI, vehicleAPI } from '../api';
import { Plus, X, CheckCircle, Wrench } from 'lucide-react';
import toast from 'react-hot-toast';

export default function Maintenance() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [showModal, setShowModal] = useState(false);
    const [vehicles, setVehicles] = useState([]);
    const [form, setForm] = useState({
        vehicleId: '', serviceType: '', description: '', cost: '', serviceDate: ''
    });

    useEffect(() => { loadLogs(); }, [page]);

    const loadLogs = async () => {
        try {
            setLoading(true);
            const res = await maintenanceAPI.getAll({ page, size: 20 });
            setLogs(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            toast.error('Failed to load maintenance logs');
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
            await maintenanceAPI.create({
                ...form,
                vehicleId: parseInt(form.vehicleId),
                cost: parseFloat(form.cost),
            });
            toast.success('Maintenance log created — Vehicle set to In Shop');
            setShowModal(false);
            setForm({ vehicleId: '', serviceType: '', description: '', cost: '', serviceDate: '' });
            loadLogs();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to create log');
        }
    };

    const handleComplete = async (id) => {
        try {
            await maintenanceAPI.complete(id);
            toast.success('Maintenance completed — Vehicle set to Available');
            loadLogs();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to complete');
        }
    };

    if (loading && logs.length === 0) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="toolbar-filters">
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        <Wrench size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                        Adding a log automatically sets vehicle to "In Shop"
                    </span>
                </div>
                <button className="btn btn-primary" onClick={openCreate}><Plus size={16} /> Log Maintenance</button>
            </div>

            <div className="data-table-wrapper">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Vehicle</th><th>Service Type</th><th>Description</th>
                            <th>Cost</th><th>Date</th><th>Status</th><th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {logs.length === 0 ? (
                            <tr><td colSpan="7" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No maintenance logs</td></tr>
                        ) : logs.map(m => (
                            <tr key={m.id}>
                                <td><strong>{m.vehicleName}</strong><br /><span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{m.vehicleLicensePlate}</span></td>
                                <td>{m.serviceType}</td>
                                <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.description || '—'}</td>
                                <td style={{ fontWeight: 600 }}>${Number(m.cost).toLocaleString()}</td>
                                <td>{m.serviceDate}</td>
                                <td>
                                    <span className={`status-pill ${m.completed ? 'completed' : 'in_shop'}`}>
                                        {m.completed ? 'Completed' : 'In Progress'}
                                    </span>
                                </td>
                                <td>
                                    {!m.completed && (
                                        <button className="btn btn-success btn-sm" onClick={() => handleComplete(m.id)} title="Complete">
                                            <CheckCircle size={14} /> Done
                                        </button>
                                    )}
                                </td>
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
                            <h3>Log Maintenance</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={handleCreate}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Vehicle *</label>
                                    <select className="form-select" value={form.vehicleId} onChange={e => setForm({ ...form, vehicleId: e.target.value })} required>
                                        <option value="">Select Vehicle</option>
                                        {vehicles.filter(v => v.status !== 'ON_TRIP').map(v => (
                                            <option key={v.id} value={v.id}>{v.name} ({v.licensePlate}) — {v.status}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Service Type *</label>
                                        <input className="form-input" value={form.serviceType} onChange={e => setForm({ ...form, serviceType: e.target.value })} required placeholder="e.g. Oil Change" />
                                    </div>
                                    <div className="form-group">
                                        <label>Cost ($) *</label>
                                        <input className="form-input" type="number" step="0.01" value={form.cost} onChange={e => setForm({ ...form, cost: e.target.value })} required />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label>Description</label>
                                    <textarea className="form-input" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Details about the service..." />
                                </div>
                                <div className="form-group">
                                    <label>Service Date *</label>
                                    <input className="form-input" type="date" value={form.serviceDate} onChange={e => setForm({ ...form, serviceDate: e.target.value })} required />
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
