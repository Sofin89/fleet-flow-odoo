import { useState, useEffect } from 'react';
import { mapAPI } from '../api';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Users, Truck } from 'lucide-react';
import toast from 'react-hot-toast';

// Fix Leaflet default icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// Driver icon (person)
const driverSVG = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`;

// Vehicle icon (truck)
const truckSVG = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M1 3h15v13H1z"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>`;

// Combined icon (truck + person)
const combinedSVG = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 3h15v13H1z"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>`;

const makeIcon = (color, svg, size = 32) => L.divIcon({
    className: '',
    html: `<div style="
        width:${size}px;height:${size}px;border-radius:50%;
        background:${color};border:3px solid #fff;
        box-shadow:0 2px 8px rgba(0,0,0,0.35);
        display:flex;align-items:center;justify-content:center;
        color:#fff;font-weight:700;font-size:12px;
    ">${svg}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size],
    popupAnchor: [0, -size],
});

// Marker icons per type
const icons = {
    DRIVER_ON: makeIcon('#27ae60', driverSVG),       // green (on duty)
    DRIVER_OFF: makeIcon('#7f8c8d', driverSVG),      // gray (off duty)
    VEHICLE_TRIP: makeIcon('#2980b9', truckSVG),      // blue (on trip)
    VEHICLE_IDLE: makeIcon('#95a5a6', truckSVG),      // light gray (parked)
    VEHICLE_SHOP: makeIcon('#e67e22', truckSVG),      // orange (in shop)
    COMBINED: makeIcon('#8e44ad', combinedSVG, 38),   // purple (driver + vehicle)
};

function getIcon(marker) {
    if (marker.markerType === 'COMBINED') return icons.COMBINED;
    if (marker.markerType === 'VEHICLE') {
        if (marker.vehicleStatus === 'ON_TRIP') return icons.VEHICLE_TRIP;
        if (marker.vehicleStatus === 'IN_SHOP') return icons.VEHICLE_SHOP;
        return icons.VEHICLE_IDLE;
    }
    return marker.dutyStatus === 'ON_DUTY' ? icons.DRIVER_ON : icons.DRIVER_OFF;
}

function FlyToMarker({ position }) {
    const map = useMap();
    useEffect(() => {
        if (position) map.flyTo([position.lat, position.lng], 14, { duration: 0.8 });
    }, [position, map]);
    return null;
}

export default function LiveMap() {
    const [markers, setMarkers] = useState([]);
    const [selectedKey, setSelectedKey] = useState(null);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');

    const fetchMarkers = async () => {
        try {
            const res = await mapAPI.getMarkers();
            setMarkers(res.data.data || []);
        } catch (err) {
            if (loading) toast.error('Failed to load map data');
        } finally { setLoading(false); }
    };

    useEffect(() => {
        fetchMarkers();
        const interval = setInterval(fetchMarkers, 5000);
        return () => clearInterval(interval);
    }, []);

    const markerKey = (m) =>
        m.markerType === 'VEHICLE' ? `v-${m.vehicleId}` :
            m.markerType === 'DRIVER' ? `d-${m.driverId}` :
                `c-${m.driverId}-${m.vehicleId}`;

    const filtered = markers.filter(m => {
        if (filter === 'ALL') return true;
        return m.markerType === filter;
    });

    const selected = filtered.find(m => markerKey(m) === selectedKey);
    const selectedPos = selected ? { lat: selected.latitude, lng: selected.longitude } : null;

    const center = markers.length > 0
        ? [
            markers.reduce((s, m) => s + m.latitude, 0) / markers.length,
            markers.reduce((s, m) => s + m.longitude, 0) / markers.length,
        ]
        : [23.0225, 72.5714];

    const counts = {
        ALL: markers.length,
        COMBINED: markers.filter(m => m.markerType === 'COMBINED').length,
        VEHICLE: markers.filter(m => m.markerType === 'VEHICLE').length,
        DRIVER: markers.filter(m => m.markerType === 'DRIVER').length,
    };

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div style={{ marginBottom: 12 }}>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-500)' }}>
                    Real-time fleet tracking. Vehicles, drivers, and combined (driver + vehicle on trip) update every 5s.
                </p>
            </div>

            {/* Filter tabs */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                {['ALL', 'COMBINED', 'VEHICLE', 'DRIVER'].map(f => (
                    <button key={f} onClick={() => setFilter(f)}
                        style={{
                            padding: '6px 14px', borderRadius: 6, fontSize: '0.78rem', fontWeight: 600,
                            border: filter === f ? '2px solid var(--primary)' : '1px solid var(--border)',
                            background: filter === f ? 'var(--primary)' : '#fff',
                            color: filter === f ? '#fff' : 'var(--text-600)',
                            cursor: 'pointer', transition: '0.15s',
                        }}>
                        {f === 'ALL' ? '🗺 All' : f === 'COMBINED' ? '🟣 Driving' : f === 'VEHICLE' ? '🔵 Vehicles' : '🟢 Drivers'} ({counts[f]})
                    </button>
                ))}
            </div>

            <div className="map-layout">
                <div className="map-panel">
                    <div className="map-panel-header">
                        <Users size={15} /> Fleet ({filtered.length})
                    </div>
                    {filtered.map(m => {
                        const key = markerKey(m);
                        const label = m.markerType === 'COMBINED'
                            ? `${m.driverName} → ${m.vehicleName}`
                            : m.markerType === 'VEHICLE'
                                ? m.vehicleName
                                : m.driverName;
                        const sub = m.markerType === 'COMBINED'
                            ? `${m.vehicleModel} · ${m.licensePlate}`
                            : m.markerType === 'VEHICLE'
                                ? `${m.vehicleModel} · ${m.vehicleStatus?.replace('_', ' ')}`
                                : `${m.licenseCategory} · ${m.dutyStatus?.replace('_', ' ')}`;
                        const typeColor = m.markerType === 'COMBINED' ? '#8e44ad' : m.markerType === 'VEHICLE' ? '#2980b9' : '#27ae60';
                        return (
                            <div key={key}
                                className={`driver-row ${selectedKey === key ? 'active' : ''}`}
                                onClick={() => setSelectedKey(key)}>
                                <div style={{ width: 10, height: 10, borderRadius: '50%', background: typeColor, flexShrink: 0 }} />
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div className="dr-name" style={{ fontSize: '0.82rem' }}>{label}</div>
                                    <div className="dr-meta">{sub}</div>
                                </div>
                                {m.speed > 0 && (
                                    <div className="dr-speed">{m.speed.toFixed(0)} km/h</div>
                                )}
                            </div>
                        );
                    })}
                </div>

                <div className="map-wrapper">
                    <MapContainer center={center} zoom={12} scrollWheelZoom={true}
                        style={{ height: '540px', width: '100%' }}>
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        {selectedPos && <FlyToMarker position={selectedPos} />}
                        {filtered.map(m => (
                            <Marker key={markerKey(m)} position={[m.latitude, m.longitude]}
                                icon={getIcon(m)}
                                eventHandlers={{ click: () => setSelectedKey(markerKey(m)) }}>
                                <Popup>
                                    <div style={{ fontFamily: 'Inter, sans-serif', minWidth: 180 }}>
                                        {m.markerType === 'COMBINED' ? (
                                            <>
                                                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#8e44ad', marginBottom: 2 }}>
                                                    🟣 Active Trip
                                                </div>
                                                <div style={{ fontSize: '0.78rem', color: '#6b7280', lineHeight: 1.7 }}>
                                                    <div><b>Driver:</b> {m.driverName}</div>
                                                    <div><b>Vehicle:</b> {m.vehicleName} ({m.vehicleModel})</div>
                                                    <div><b>Plate:</b> {m.licensePlate}</div>
                                                    <div><b>Safety:</b> {m.safetyScore}/100</div>
                                                    <div style={{ color: '#8e44ad', fontWeight: 600 }}>Speed: {m.speed?.toFixed(1)} km/h</div>
                                                </div>
                                            </>
                                        ) : m.markerType === 'VEHICLE' ? (
                                            <>
                                                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#2980b9', marginBottom: 2 }}>
                                                    🔵 {m.vehicleName}
                                                </div>
                                                <div style={{ fontSize: '0.78rem', color: '#6b7280', lineHeight: 1.7 }}>
                                                    <div>{m.vehicleModel}</div>
                                                    <div>Plate: {m.licensePlate}</div>
                                                    <div>Type: {m.vehicleType}</div>
                                                    <div>Status: {m.vehicleStatus?.replace('_', ' ')}</div>
                                                </div>
                                            </>
                                        ) : (
                                            <>
                                                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#27ae60', marginBottom: 2 }}>
                                                    🟢 {m.driverName}
                                                </div>
                                                <div style={{ fontSize: '0.78rem', color: '#6b7280', lineHeight: 1.7 }}>
                                                    <div>License: {m.licenseNumber}</div>
                                                    <div>Category: {m.licenseCategory}</div>
                                                    <div>Status: {m.dutyStatus?.replace('_', ' ')}</div>
                                                    <div>Safety: {m.safetyScore}/100</div>
                                                    {m.speed > 0 && <div style={{ color: '#27ae60', fontWeight: 600 }}>Speed: {m.speed?.toFixed(1)} km/h</div>}
                                                </div>
                                            </>
                                        )}
                                        <div style={{ fontSize: '0.7rem', color: '#9ca3af', marginTop: 4 }}>
                                            Updated: {new Date(m.lastUpdated).toLocaleTimeString()}
                                        </div>
                                    </div>
                                </Popup>
                            </Marker>
                        ))}
                    </MapContainer>
                </div>
            </div>
        </div>
    );
}
