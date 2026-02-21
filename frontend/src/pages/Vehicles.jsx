import { useState, useEffect } from 'react';
import { vehicleAPI } from '../api';
import { Plus, Edit, Truck, X } from 'lucide-react';
import toast from 'react-hot-toast';

const statusOptions = ['AVAILABLE', 'ON_TRIP', 'IN_SHOP', 'RETIRED'];
const typeOptions = ['TRUCK', 'VAN', 'BIKE'];

export default function Vehicles() {
    const [vehicles, setVehicles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [showModal, setShowModal] = useState(false);
    const [editingVehicle, setEditingVehicle] = useState(null);
    const [filterStatus, setFilterStatus] = useState('');
    const [filterType, setFilterType] = useState('');
    const [form, setForm] = useState({
        name: '', model: '', licensePlate: '', vehicleType: 'VAN',
        maxLoadCapacityKg: '', odometerKm: '0', region: '', acquisitionCost: ''
    });

    useEffect(() => { loadVehicles(); }, [page, filterStatus, filterType]);

    const loadVehicles = async () => {
        try {
            setLoading(true);
            const params = { page, size: 20 };
            if (filterStatus) params.status = filterStatus;
            if (filterType) params.type = filterType;
            const res = await vehicleAPI.getAll(params);
            setVehicles(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            toast.error('Failed to load vehicles');
        } finally { setLoading(false); }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                ...form,
                maxLoadCapacityKg: parseFloat(form.maxLoadCapacityKg),
                odometerKm: parseFloat(form.odometerKm || '0'),
                acquisitionCost: form.acquisitionCost ? parseFloat(form.acquisitionCost) : null,
            };
            if (editingVehicle) {
                await vehicleAPI.update(editingVehicle.id, payload);
                toast.success('Vehicle updated');
            } else {
                await vehicleAPI.create(payload);
                toast.success('Vehicle created');
            }
            setShowModal(false);
            resetForm();
            loadVehicles();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Operation failed');
        }
    };

    const handleStatusChange = async (id, status) => {
        try {
            await vehicleAPI.updateStatus(id, status);
            toast.success('Status updated');
            loadVehicles();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update status');
        }
    };

    const openEdit = (v) => {
        setEditingVehicle(v);
        setForm({
            name: v.name, model: v.model, licensePlate: v.licensePlate,
            vehicleType: v.vehicleType, maxLoadCapacityKg: v.maxLoadCapacityKg,
            odometerKm: v.odometerKm, region: v.region || '', acquisitionCost: v.acquisitionCost || ''
        });
        setShowModal(true);
    };

    const resetForm = () => {
        setEditingVehicle(null);
        setForm({ name: '', model: '', licensePlate: '', vehicleType: 'VAN', maxLoadCapacityKg: '', odometerKm: '0', region: '', acquisitionCost: '' });
    };

    if (loading && vehicles.length === 0) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="toolbar-filters">
                    <select className="form-select" value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(0); }}>
                        <option value="">All Status</option>
                        {statusOptions.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select>
                    <select className="form-select" value={filterType} onChange={e => { setFilterType(e.target.value); setPage(0); }}>
                        <option value="">All Types</option>
                        {typeOptions.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                </div>
                <button className="btn btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
                    <Plus size={16} /> Add Vehicle
                </button>
            </div>

            <div className="data-table-wrapper">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Vehicle</th><th>License Plate</th><th>Type</th><th>Max Load (kg)</th>
                            <th>Odometer (km)</th><th>Region</th><th>Status</th><th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {vehicles.length === 0 ? (
                            <tr><td colSpan="8" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No vehicles found</td></tr>
                        ) : vehicles.map(v => (
                            <tr key={v.id}>
                                <td><strong>{v.name}</strong><br /><span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{v.model}</span></td>
                                <td style={{ fontFamily: 'monospace' }}>{v.licensePlate}</td>
                                <td>{v.vehicleType}</td>
                                <td>{Number(v.maxLoadCapacityKg).toLocaleString()}</td>
                                <td>{Number(v.odometerKm).toLocaleString()}</td>
                                <td>{v.region || '—'}</td>
                                <td>
                                    <select className="form-select" value={v.status}
                                        onChange={e => handleStatusChange(v.id, e.target.value)}
                                        style={{
                                            padding: '4px 8px', minWidth: 'auto', fontSize: '0.75rem', width: 'auto',
                                            background: 'transparent', border: 'none', color: 'inherit',
                                            cursor: 'pointer'
                                        }}>
                                        {statusOptions.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                                    </select>
                                    <span className={`status-pill ${v.status.toLowerCase()}`}>{v.status.replace('_', ' ')}</span>
                                </td>
                                <td>
                                    <div className="action-buttons">
                                        <button className="btn btn-secondary btn-sm" onClick={() => openEdit(v)}><Edit size={14} /></button>
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
                            <h3>{editingVehicle ? 'Edit Vehicle' : 'Add Vehicle'}</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="modal-body">
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Vehicle Name *</label>
                                        <input className="form-input" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required placeholder="e.g. Van-05" />
                                    </div>
                                    <div className="form-group">
                                        <label>Model *</label>
                                        <input className="form-input" value={form.model} onChange={e => setForm({ ...form, model: e.target.value })} required placeholder="e.g. Ford Transit" />
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>License Plate *</label>
                                        <input className="form-input" value={form.licensePlate} onChange={e => setForm({ ...form, licensePlate: e.target.value })} required placeholder="e.g. FL-V005" />
                                    </div>
                                    <div className="form-group">
                                        <label>Vehicle Type *</label>
                                        <select className="form-select" value={form.vehicleType} onChange={e => setForm({ ...form, vehicleType: e.target.value })}>
                                            {typeOptions.map(t => <option key={t} value={t}>{t}</option>)}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Max Load Capacity (kg) *</label>
                                        <input className="form-input" type="number" step="0.01" value={form.maxLoadCapacityKg} onChange={e => setForm({ ...form, maxLoadCapacityKg: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label>Odometer (km)</label>
                                        <input className="form-input" type="number" step="0.01" value={form.odometerKm} onChange={e => setForm({ ...form, odometerKm: e.target.value })} />
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Region</label>
                                        <input className="form-input" value={form.region} onChange={e => setForm({ ...form, region: e.target.value })} placeholder="e.g. North" />
                                    </div>
                                    <div className="form-group">
                                        <label>Acquisition Cost ($)</label>
                                        <input className="form-input" type="number" step="0.01" value={form.acquisitionCost} onChange={e => setForm({ ...form, acquisitionCost: e.target.value })} />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingVehicle ? 'Update' : 'Create'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
