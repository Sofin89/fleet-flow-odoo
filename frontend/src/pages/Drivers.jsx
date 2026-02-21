import { useState, useEffect } from 'react';
import { driverAPI } from '../api';
import { Plus, Edit, X, Shield, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const dutyOptions = ['ON_DUTY', 'OFF_DUTY', 'SUSPENDED'];
const licCatOptions = ['TRUCK', 'VAN', 'BIKE'];

export default function Drivers() {
    const [drivers, setDrivers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [showModal, setShowModal] = useState(false);
    const [editingDriver, setEditingDriver] = useState(null);
    const [filterStatus, setFilterStatus] = useState('');
    const [filterCategory, setFilterCategory] = useState('');
    const [form, setForm] = useState({
        fullName: '', licenseNumber: '', licenseCategory: 'VAN',
        licenseExpiry: '', phone: '', email: '', dutyStatus: 'OFF_DUTY'
    });

    useEffect(() => { loadDrivers(); }, [page, filterStatus, filterCategory]);

    const loadDrivers = async () => {
        try {
            setLoading(true);
            const params = { page, size: 20 };
            if (filterStatus) params.status = filterStatus;
            if (filterCategory) params.category = filterCategory;
            const res = await driverAPI.getAll(params);
            setDrivers(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            toast.error('Failed to load drivers');
        } finally { setLoading(false); }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingDriver) {
                await driverAPI.update(editingDriver.id, form);
                toast.success('Driver updated');
            } else {
                await driverAPI.create(form);
                toast.success('Driver created');
            }
            setShowModal(false);
            resetForm();
            loadDrivers();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Operation failed');
        }
    };

    const handleStatusChange = async (id, status) => {
        try {
            await driverAPI.updateStatus(id, status);
            toast.success('Status updated');
            loadDrivers();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Status update failed');
        }
    };

    const openEdit = (d) => {
        setEditingDriver(d);
        setForm({
            fullName: d.fullName, licenseNumber: d.licenseNumber,
            licenseCategory: d.licenseCategory, licenseExpiry: d.licenseExpiry,
            phone: d.phone || '', email: d.email || '', dutyStatus: d.dutyStatus
        });
        setShowModal(true);
    };

    const resetForm = () => {
        setEditingDriver(null);
        setForm({ fullName: '', licenseNumber: '', licenseCategory: 'VAN', licenseExpiry: '', phone: '', email: '', dutyStatus: 'OFF_DUTY' });
    };

    const getSafetyColor = (score) => {
        if (score >= 90) return '#22c55e';
        if (score >= 70) return '#f59e0b';
        return '#ef4444';
    };

    if (loading && drivers.length === 0) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="toolbar-filters">
                    <select className="form-select" value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(0); }}>
                        <option value="">All Status</option>
                        {dutyOptions.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select>
                    <select className="form-select" value={filterCategory} onChange={e => { setFilterCategory(e.target.value); setPage(0); }}>
                        <option value="">All License</option>
                        {licCatOptions.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                </div>
                <button className="btn btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
                    <Plus size={16} /> Add Driver
                </button>
            </div>

            <div className="data-table-wrapper">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Driver</th><th>License</th><th>Category</th><th>Expiry</th>
                            <th>Safety Score</th><th>Completion Rate</th><th>Status</th><th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {drivers.length === 0 ? (
                            <tr><td colSpan="8" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No drivers found</td></tr>
                        ) : drivers.map(d => (
                            <tr key={d.id}>
                                <td>
                                    <strong>{d.fullName}</strong>
                                    {d.email && <><br /><span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{d.email}</span></>}
                                </td>
                                <td style={{ fontFamily: 'monospace' }}>{d.licenseNumber}</td>
                                <td>{d.licenseCategory}</td>
                                <td>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                                        {!d.licenseValid && <AlertCircle size={14} color="#ef4444" />}
                                        <span style={{ color: d.licenseValid ? 'var(--text-secondary)' : '#ef4444' }}>{d.licenseExpiry}</span>
                                    </div>
                                </td>
                                <td>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                        <Shield size={14} color={getSafetyColor(d.safetyScore)} />
                                        <span style={{ color: getSafetyColor(d.safetyScore), fontWeight: 600 }}>{d.safetyScore}</span>
                                    </div>
                                </td>
                                <td>{d.tripCompletionRate?.toFixed(1)}%</td>
                                <td>
                                    <select className="form-select" value={d.dutyStatus}
                                        onChange={e => handleStatusChange(d.id, e.target.value)}
                                        style={{
                                            padding: '4px 8px', minWidth: 'auto', fontSize: '0.75rem', width: 'auto',
                                            background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer'
                                        }}>
                                        {dutyOptions.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                                    </select>
                                    <span className={`status-pill ${d.dutyStatus.toLowerCase()}`}>{d.dutyStatus.replace('_', ' ')}</span>
                                </td>
                                <td>
                                    <div className="action-buttons">
                                        <button className="btn btn-secondary btn-sm" onClick={() => openEdit(d)}><Edit size={14} /></button>
                                    </div>
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
                            <h3>{editingDriver ? 'Edit Driver' : 'Add Driver'}</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Full Name *</label>
                                    <input className="form-input" value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} required />
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>License Number *</label>
                                        <input className="form-input" value={form.licenseNumber} onChange={e => setForm({ ...form, licenseNumber: e.target.value })} required disabled={!!editingDriver} />
                                    </div>
                                    <div className="form-group">
                                        <label>License Category *</label>
                                        <select className="form-select" value={form.licenseCategory} onChange={e => setForm({ ...form, licenseCategory: e.target.value })}>
                                            {licCatOptions.map(c => <option key={c} value={c}>{c}</option>)}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>License Expiry *</label>
                                        <input className="form-input" type="date" value={form.licenseExpiry} onChange={e => setForm({ ...form, licenseExpiry: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label>Duty Status</label>
                                        <select className="form-select" value={form.dutyStatus} onChange={e => setForm({ ...form, dutyStatus: e.target.value })}>
                                            {dutyOptions.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Phone</label>
                                        <input className="form-input" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                        <label>Email</label>
                                        <input className="form-input" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingDriver ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
