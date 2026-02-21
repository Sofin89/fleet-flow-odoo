import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { tripAPI, vehicleAPI, driverAPI } from '../api';
import { Plus, X, Play, CheckCircle, XCircle, Edit } from 'lucide-react';
import toast from 'react-hot-toast';
import MapLocationPicker from '../components/MapLocationPicker';

const statusOptions = ['DRAFT', 'DISPATCHED', 'COMPLETED', 'CANCELLED'];

export default function Trips() {
    const { user } = useAuth();
    const isAnalyst = user?.role === 'ANALYST';
    const isDriver = user?.role === 'DRIVER';
    const [trips, setTrips] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [showModal, setShowModal] = useState(false);
    const [showCompleteModal, setShowCompleteModal] = useState(false);
    const [showMapPicker, setShowMapPicker] = useState(false);
    const [completingTrip, setCompletingTrip] = useState(null);
    const [editingTrip, setEditingTrip] = useState(null);
    const [filterStatus, setFilterStatus] = useState('');
    const [vehicles, setVehicles] = useState([]);
    const [drivers, setDrivers] = useState([]);
    const [form, setForm] = useState({
        vehicleId: '', driverId: '', origin: '', destination: '', originName: '', destinationName: '', cargoWeightKg: '', revenue: ''
    });
    const [completeForm, setCompleteForm] = useState({ endOdometer: '', revenue: '' });

    useEffect(() => { loadTrips(); }, [page, filterStatus]);

    const loadTrips = async () => {
        try {
            setLoading(true);
            const params = { page, size: 20 };
            if (filterStatus) params.status = filterStatus;
            const res = await tripAPI.getAll(params);
            setTrips(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            toast.error('Failed to load trips');
        } finally { setLoading(false); }
    };

    const loadFormData = async () => {
        try {
            const [vRes, dRes] = await Promise.all([vehicleAPI.getAvailable(), driverAPI.getAvailable()]);
            setVehicles(vRes.data.data || []);
            setDrivers(dRes.data.data || []);
        } catch (err) {
            toast.error('Failed to load form data');
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            await tripAPI.create({
                ...form,
                vehicleId: parseInt(form.vehicleId),
                driverId: parseInt(form.driverId),
                cargoWeightKg: parseFloat(form.cargoWeightKg),
                revenue: form.revenue ? parseFloat(form.revenue) : null,
                originName: form.originName || null,
                destinationName: form.destinationName || null,
            });
            toast.success('Trip created');
            setShowModal(false);
            setForm({ vehicleId: '', driverId: '', origin: '', destination: '', originName: '', destinationName: '', cargoWeightKg: '', revenue: '' });
            loadTrips();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to create trip');
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        try {
            await tripAPI.update(editingTrip.id, {
                ...form,
                vehicleId: parseInt(form.vehicleId),
                driverId: parseInt(form.driverId),
                cargoWeightKg: parseFloat(form.cargoWeightKg),
                revenue: form.revenue ? parseFloat(form.revenue) : null,
                originName: form.originName || null,
                destinationName: form.destinationName || null,
            });
            toast.success('Trip updated');
            setShowModal(false);
            setEditingTrip(null);
            setForm({ vehicleId: '', driverId: '', origin: '', destination: '', originName: '', destinationName: '', cargoWeightKg: '', revenue: '' });
            loadTrips();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update trip');
        }
    };

    const handleDispatch = async (id) => {
        try {
            await tripAPI.dispatch(id);
            toast.success('Trip dispatched');
            loadTrips();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to dispatch');
        }
    };

    const handleComplete = async (e) => {
        e.preventDefault();
        try {
            await tripAPI.complete(completingTrip.id, {
                endOdometer: completeForm.endOdometer ? parseFloat(completeForm.endOdometer) : null,
                revenue: completeForm.revenue ? parseFloat(completeForm.revenue) : null,
            });
            toast.success('Trip completed');
            setShowCompleteModal(false);
            setCompletingTrip(null);
            loadTrips();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to complete trip');
        }
    };

    const handleCancel = async (id) => {
        if (!confirm('Cancel this trip?')) return;
        try {
            await tripAPI.cancel(id);
            toast.success('Trip cancelled');
            loadTrips();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to cancel');
        }
    };

    const openCreate = async () => {
        await loadFormData();
        setEditingTrip(null);
        setForm({ vehicleId: '', driverId: '', origin: '', destination: '', originName: '', destinationName: '', cargoWeightKg: '', revenue: '' });
        setShowModal(true);
    };

    const openEdit = async (trip) => {
        await loadFormData();
        setEditingTrip(trip);
        setForm({
            vehicleId: trip.vehicleId.toString(),
            driverId: trip.driverId.toString(),
            origin: trip.origin,
            destination: trip.destination,
            originName: trip.originName || '',
            destinationName: trip.destinationName || '',
            cargoWeightKg: trip.cargoWeightKg.toString(),
            revenue: trip.revenue ? trip.revenue.toString() : '',
        });
        setShowModal(true);
    };

    const openComplete = (trip) => {
        setCompletingTrip(trip);
        setCompleteForm({ endOdometer: '', revenue: trip.revenue || '' });
        setShowCompleteModal(true);
    };

    const handleMapSelection = (locations) => {
        setForm({
            ...form,
            origin: locations.origin,
            destination: locations.destination,
            originName: locations.originName,
            destinationName: locations.destinationName,
        });
        setShowMapPicker(false);
    };

    if (loading && trips.length === 0) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div className="page-toolbar">
                <div className="toolbar-filters">
                    <select className="form-select" value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(0); }}>
                        <option value="">All Statuses</option>
                        {statusOptions.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                </div>
                {!isAnalyst && !isDriver && (
                    <button className="btn btn-primary" onClick={openCreate}><Plus size={16} /> Create Trip</button>
                )}
            </div>

            <div className="data-table-wrapper">
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>ID</th><th>Route</th><th>Vehicle</th><th>Driver</th>
                            <th>Cargo (kg)</th><th>Revenue</th><th>Status</th><th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {trips.length === 0 ? (
                            <tr><td colSpan="8" style={{ textAlign: 'center', padding: 40, color: 'var(--text-muted)' }}>No trips found</td></tr>
                        ) : trips.map(t => (
                            <tr key={t.id}>
                                <td>#{t.id}</td>
                                <td>
                                    <div style={{ marginBottom: 4 }}>
                                        <strong>{t.originName || t.origin}</strong> → {t.destinationName || t.destination}
                                    </div>
                                    <a
                                        href={`https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(t.origin)}&destination=${encodeURIComponent(t.destination)}`}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        style={{
                                            display: 'inline-flex',
                                            alignItems: 'center',
                                            fontSize: '0.7rem',
                                            color: '#2980b9',
                                            textDecoration: 'none',
                                            background: '#e3f2fd',
                                            padding: '3px 8px',
                                            borderRadius: 12,
                                            fontWeight: 600
                                        }}
                                        title="Open in Google Maps"
                                    >
                                        🗺️ Google Maps
                                    </a>
                                </td>
                                <td>{t.vehicleName}<br /><span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{t.vehicleLicensePlate}</span></td>
                                <td>{t.driverName}</td>
                                <td>
                                    <span>{Number(t.cargoWeightKg).toLocaleString()}</span>
                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}> / {Number(t.maxCapacityKg).toLocaleString()}</span>
                                </td>
                                <td>{t.revenue ? `$${Number(t.revenue).toLocaleString()}` : '—'}</td>
                                <td><span className={`status-pill ${t.status.toLowerCase()}`}>{t.status}</span></td>
                                <td>
                                    <div className="action-buttons">
                                        {t.status === 'DRAFT' && !isAnalyst && !isDriver && (
                                            <>
                                                <button className="btn btn-primary btn-sm" onClick={() => openEdit(t)} title="Edit"><Edit size={14} /></button>
                                                <button className="btn btn-success btn-sm" onClick={() => handleDispatch(t.id)} title="Dispatch"><Play size={14} /></button>
                                                <button className="btn btn-danger btn-sm" onClick={() => handleCancel(t.id)} title="Cancel"><XCircle size={14} /></button>
                                            </>
                                        )}
                                        {t.status === 'DISPATCHED' && !isAnalyst && !isDriver && (
                                            <>
                                                <button className="btn btn-success btn-sm" onClick={() => openComplete(t)} title="Complete"><CheckCircle size={14} /></button>
                                                <button className="btn btn-danger btn-sm" onClick={() => handleCancel(t.id)} title="Cancel"><XCircle size={14} /></button>
                                            </>
                                        )}
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

            {/* Create/Edit Trip Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>{editingTrip ? 'Edit Trip' : 'Create Trip'}</h3>
                            <button className="modal-close" onClick={() => setShowModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={editingTrip ? handleUpdate : handleCreate}>
                            <div className="modal-body">
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Vehicle *</label>
                                        <select className="form-select" value={form.vehicleId} onChange={e => setForm({ ...form, vehicleId: e.target.value })} required>
                                            <option value="">Select Vehicle</option>
                                            {vehicles.map(v => (
                                                <option key={v.id} value={v.id}>{v.name} ({v.licensePlate}) — Max: {v.maxLoadCapacityKg}kg</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label>Driver *</label>
                                        <select className="form-select" value={form.driverId} onChange={e => setForm({ ...form, driverId: e.target.value })} required>
                                            <option value="">Select Driver</option>
                                            {drivers.map(d => (
                                                <option key={d.id} value={d.id}>{d.fullName} ({d.licenseCategory})</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Origin *</label>
                                        <input className="form-input" value={form.origin} onChange={e => setForm({ ...form, origin: e.target.value })} required placeholder="Warehouse A" />
                                        {form.originName && (
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>
                                                📍 {form.originName}
                                            </div>
                                        )}
                                    </div>
                                    <div className="form-group">
                                        <label>Destination *</label>
                                        <input className="form-input" value={form.destination} onChange={e => setForm({ ...form, destination: e.target.value })} required placeholder="Depot B" />
                                        {form.destinationName && (
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>
                                                🏁 {form.destinationName}
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div style={{ marginBottom: 16 }}>
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => setShowMapPicker(true)}
                                        style={{ width: '100%' }}>
                                        🗺️ Select on Map
                                    </button>
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Cargo Weight (kg) *</label>
                                        <input className="form-input" type="number" step="0.01" value={form.cargoWeightKg} onChange={e => setForm({ ...form, cargoWeightKg: e.target.value })} required />
                                    </div>
                                    <div className="form-group">
                                        <label>Revenue ($)</label>
                                        <input className="form-input" type="number" step="0.01" value={form.revenue} onChange={e => setForm({ ...form, revenue: e.target.value })} />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">{editingTrip ? 'Update Trip' : 'Create Trip'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Complete Trip Modal */}
            {showCompleteModal && (
                <div className="modal-overlay" onClick={() => setShowCompleteModal(false)}>
                    <div className="modal" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Complete Trip #{completingTrip?.id}</h3>
                            <button className="modal-close" onClick={() => setShowCompleteModal(false)}><X size={20} /></button>
                        </div>
                        <form onSubmit={handleComplete}>
                            <div className="modal-body">
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>End Odometer (km)</label>
                                        <input className="form-input" type="number" step="0.01" value={completeForm.endOdometer} onChange={e => setCompleteForm({ ...completeForm, endOdometer: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                        <label>Revenue ($)</label>
                                        <input className="form-input" type="number" step="0.01" value={completeForm.revenue} onChange={e => setCompleteForm({ ...completeForm, revenue: e.target.value })} />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowCompleteModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-success">Complete Trip</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Map Location Picker Modal */}
            {showMapPicker && (
                <MapLocationPicker
                    onConfirm={handleMapSelection}
                    onCancel={() => setShowMapPicker(false)}
                />
            )}
        </div>
    );
}
