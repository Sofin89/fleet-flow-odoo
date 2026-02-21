import { useState, useEffect, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { mapAPI } from '../api';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Users, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import webSocketService from '../services/WebSocketService';

// Fix Leaflet default icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// SVG icons
const driverSVG = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`;
const truckSVG = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M1 3h15v13H1z"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>`;
const combinedSVG = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 3h15v13H1z"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>`;

const makeIcon = (color, svg, size = 32, isMe = false) => L.divIcon({
    className: '',
    html: `<div style="
        width:${size}px;height:${size}px;border-radius:50%;
        background:${color};border:${isMe ? '4px' : '3px'} solid #fff;
        box-shadow:0 2px 8px rgba(0,0,0,0.35);
        display:flex;align-items:center;justify-content:center;
        color:#fff;font-weight:700;font-size:12px;
        ${isMe ? 'animation: pulse 2s infinite;' : ''}
    ">${svg}</div>
    ${isMe ? `<style>
        @keyframes pulse {
            0%, 100% { box-shadow: 0 2px 8px rgba(0,0,0,0.35), 0 0 0 0 rgba(231, 76, 60, 0.7); }
            50% { box-shadow: 0 2px 8px rgba(0,0,0,0.35), 0 0 0 10px rgba(231, 76, 60, 0); }
        }
    </style>` : ''}`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size],
    popupAnchor: [0, -size],
});

const pinIcon = (emoji) => L.divIcon({
    className: '',
    html: `<div style="font-size:18px;text-shadow:0 1px 3px rgba(0,0,0,0.4);">${emoji}</div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
});

const warningIcon = L.divIcon({
    className: '',
    html: `<div style="font-size:20px;text-shadow:0 1px 3px rgba(0,0,0,0.4);">⚠️</div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
});

const icons = {
    DRIVER_ON: makeIcon('#27ae60', driverSVG),
    DRIVER_OFF: makeIcon('#7f8c8d', driverSVG),
    ME_MARKER: makeIcon('#e74c3c', driverSVG, 38, true), // Red, larger, with pulse animation
    VEHICLE_TRIP: makeIcon('#2980b9', truckSVG),
    VEHICLE_IDLE: makeIcon('#95a5a6', truckSVG),
    VEHICLE_SHOP: makeIcon('#e67e22', truckSVG),
    COMBINED: makeIcon('#8e44ad', combinedSVG, 38),
    ME_COMBINED: makeIcon('#8e44ad', combinedSVG, 44, true), // Purple, larger, with pulse animation
    ORIGIN: pinIcon('📍'),
    DEST: pinIcon('🏁'),
};

function getIcon(marker, currentUserId) {
    // Check if this is the current user's marker
    if (marker.driverId === currentUserId) {
        if (marker.markerType === 'COMBINED') return icons.ME_COMBINED;
        if (marker.markerType === 'DRIVER') return icons.ME_MARKER;
    }

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

// Auto-center on driver's location (Me Marker)
function AutoCenterOnDriver({ driverId, markers }) {
    const map = useMap();
    const [hasManuallyPanned, setHasManuallyPanned] = useState(false);
    const initialCenterDone = useRef(false);

    // Find the driver's marker
    const driverMarker = useMemo(() => {
        return markers.find(m => (m.markerType === 'DRIVER' || m.markerType === 'COMBINED') && m.driverId === driverId);
    }, [markers, driverId]);

    // Initial center on mount
    useEffect(() => {
        if (!initialCenterDone.current && driverMarker && !hasManuallyPanned) {
            map.setView([driverMarker.latitude, driverMarker.longitude], 14);
            initialCenterDone.current = true;
        }
    }, [driverMarker, map, hasManuallyPanned]);

    // Listen for manual panning
    useEffect(() => {
        const handleDragStart = () => setHasManuallyPanned(true);
        map.on('dragstart', handleDragStart);
        return () => map.off('dragstart', handleDragStart);
    }, [map]);

    return null;
}

function formatETA(minutes) {
    if (!minutes || minutes <= 0) return 'Arrived';
    if (minutes < 60) return `${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

function Toggle({ checked, onChange, label }) {
    return (
        <div onClick={onChange} style={{
            display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer',
            fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-600)',
            padding: '6px 12px', borderRadius: 6,
            background: checked ? 'rgba(142, 68, 173, 0.08)' : 'transparent',
            border: `1px solid ${checked ? '#8e44ad' : 'var(--border)'}`,
            transition: '0.2s', userSelect: 'none',
        }}>
            <div style={{
                width: 34, height: 18, borderRadius: 9, padding: 2,
                background: checked ? '#8e44ad' : '#ccc', transition: '0.2s',
                display: 'flex', alignItems: 'center',
                justifyContent: checked ? 'flex-end' : 'flex-start',
            }}>
                <div style={{
                    width: 14, height: 14, borderRadius: '50%',
                    background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
                }} />
            </div>
            {label}
        </div>
    );
}

// ─── Polyline Helpers ─────────────────────────────────────
function closestPointIndex(polyline, lat, lng) {
    let minDist = Infinity, bestIdx = 0;
    for (let i = 0; i < polyline.length; i++) {
        const dLat = polyline[i][0] - lat;
        const dLng = polyline[i][1] - lng;
        const dist = dLat * dLat + dLng * dLng;
        if (dist < minDist) { minDist = dist; bestIdx = i; }
    }
    return bestIdx;
}
// ───────────────────────────────────────────────────────────────────

export default function LiveMap() {
    const { user } = useAuth();
    const isAnalyst = user?.role === 'ANALYST';
    const [markers, setMarkers] = useState([]);
    const [selectedKey, setSelectedKey] = useState(null);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');
    const [search, setSearch] = useState('');
    const [showRoutes, setShowRoutes] = useState(true);
    const [speedFilter, setSpeedFilter] = useState('ALL');
    // No longer need separate route polylines state - using backend data

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

        let locationSub = null;

        // Connect to WebSocket for real-time updates
        webSocketService.connect(
            () => {
                console.log('WebSocket connected for LiveMap');

                // Subscribe to location updates
                locationSub = webSocketService.subscribe('/topic/locations', (event) => {
                    handleLocationUpdate(event);
                });
            },
            (error) => {
                console.error('WebSocket connection error:', error);
                // Continue with polling fallback
            }
        );

        return () => {
            clearInterval(interval);
            if (locationSub) {
                webSocketService.unsubscribe(locationSub);
            }
            webSocketService.disconnect();
        };
    }, []);

    // Handle real-time location updates from WebSocket
    const handleLocationUpdate = (event) => {
        if (event.type === 'LOCATION_UPDATE') {
            // Update the marker in the markers array
            setMarkers(prevMarkers => {
                const markerIndex = prevMarkers.findIndex(m =>
                    m.markerType === 'DRIVER' && m.driverId === event.driverId
                );

                if (markerIndex >= 0) {
                    // Update existing marker
                    const updatedMarkers = [...prevMarkers];
                    updatedMarkers[markerIndex] = {
                        ...updatedMarkers[markerIndex],
                        latitude: event.latitude,
                        longitude: event.longitude,
                        accuracy: event.accuracy,
                        lastUpdated: event.timestamp
                    };
                    return updatedMarkers;
                } else {
                    // Marker not found - will be picked up by next poll
                    return prevMarkers;
                }
            });
        } else if (event.type === 'SHARING_STATUS_CHANGE') {
            // Update sharing status
            setMarkers(prevMarkers => {
                const markerIndex = prevMarkers.findIndex(m =>
                    m.markerType === 'DRIVER' && m.driverId === event.driverId
                );

                if (markerIndex >= 0) {
                    const updatedMarkers = [...prevMarkers];
                    updatedMarkers[markerIndex] = {
                        ...updatedMarkers[markerIndex],
                        sharingActive: event.active
                    };
                    return updatedMarkers;
                }

                return prevMarkers;
            });
        }
    };

    const markerKey = (m) =>
        m.markerType === 'VEHICLE' ? `v-${m.vehicleId}` :
            m.markerType === 'DRIVER' ? `d-${m.driverId}` :
                `c-${m.driverId}-${m.vehicleId}`;

    const filtered = useMemo(() => {
        return markers.filter(m => {
            if (filter !== 'ALL' && m.markerType !== filter) return false;
            if (speedFilter === 'MOVING' && (m.speed || 0) <= 0) return false;
            if (speedFilter === 'STOPPED' && (m.speed || 0) > 0) return false;
            if (search) {
                const q = search.toLowerCase();
                const fields = [m.driverName, m.vehicleName, m.vehicleModel,
                m.licensePlate, m.tripOrigin, m.tripDestination
                ].filter(Boolean).join(' ').toLowerCase();
                if (!fields.includes(q)) return false;
            }
            return true;
        });
    }, [markers, filter, speedFilter, search]);

    const selected = filtered.find(m => markerKey(m) === selectedKey);
    const selectedPos = selected ? { lat: selected.latitude, lng: selected.longitude } : null;

    // Determine map center
    // For drivers, try to center on their location; otherwise use fleet center
    const center = useMemo(() => {
        const currentDriverId = user?.driverId || user?.userId;
        if (user?.role === 'DRIVER' && markers.length > 0 && currentDriverId) {
            const driverMarker = markers.find(m => (m.markerType === 'DRIVER' || m.markerType === 'COMBINED') && m.driverId === currentDriverId);
            if (driverMarker) {
                return [driverMarker.latitude, driverMarker.longitude];
            }
        }

        // Default: center on all markers or default location
        if (markers.length > 0) {
            return [
                markers.reduce((s, m) => s + m.latitude, 0) / markers.length,
                markers.reduce((s, m) => s + m.longitude, 0) / markers.length,
            ];
        }

        return [23.0225, 72.5714]; // Default Ahmedabad location
    }, [markers, user]);

    const counts = {
        ALL: markers.length,
        COMBINED: markers.filter(m => m.markerType === 'COMBINED').length,
        VEHICLE: markers.filter(m => m.markerType === 'VEHICLE').length,
        DRIVER: markers.filter(m => m.markerType === 'DRIVER').length,
    };

    // Build split route lines from backend route data
    const routeLines = useMemo(() => {
        if (!showRoutes) return [];
        return filtered
            .filter(m => m.markerType === 'COMBINED' && m.routePolyline)
            .map(m => {
                const fullRoute = m.routePolyline;
                const driverPos = [m.latitude, m.longitude];
                const splitIdx = closestPointIndex(fullRoute, m.latitude, m.longitude);

                return {
                    key: markerKey(m),
                    completed: fullRoute.slice(0, splitIdx + 1),
                    remaining: fullRoute.slice(splitIdx),
                    snappedPos: fullRoute[splitIdx],
                    originPos: fullRoute[0],
                    destPos: fullRoute[fullRoute.length - 1],
                    origin: m.tripOrigin,
                    dest: m.tripDestination,
                    isFallback: m.isRouteFallback,
                };
            });
    }, [filtered, showRoutes]);

    if (loading) return <div className="loading-spinner"><div className="spinner" /></div>;

    return (
        <div>
            <div style={{ marginBottom: 12 }}>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-500)' }}>
                    Real-time fleet tracking. Vehicles, drivers, and combined (driver + vehicle on trip) update every 5s.
                </p>
            </div>

            {/* Controls row */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 10, flexWrap: 'wrap', alignItems: 'center' }}>
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
                <div style={{ width: 1, height: 24, background: 'var(--border)', margin: '0 4px' }} />
                <Toggle checked={showRoutes} onChange={() => setShowRoutes(v => !v)} label="🛣️ Routes" />
            </div>

            {/* Secondary filters */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 16, alignItems: 'center' }}>
                <div style={{ position: 'relative', flex: 1, maxWidth: 260 }}>
                    <Search size={14} style={{
                        position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)',
                        color: 'var(--text-400)', pointerEvents: 'none',
                    }} />
                    <input type="text" placeholder="Search name, plate, route..."
                        value={search} onChange={e => setSearch(e.target.value)}
                        style={{
                            width: '100%', padding: '7px 10px 7px 30px', borderRadius: 6,
                            border: '1px solid var(--border)', fontSize: '0.78rem',
                            background: '#fff', outline: 'none',
                        }} />
                </div>
                {['ALL', 'MOVING', 'STOPPED'].map(s => (
                    <button key={s} onClick={() => setSpeedFilter(s)}
                        style={{
                            padding: '5px 10px', borderRadius: 5, fontSize: '0.72rem', fontWeight: 600,
                            border: speedFilter === s ? '1.5px solid var(--primary)' : '1px solid var(--border)',
                            background: speedFilter === s ? 'rgba(41, 128, 185, 0.08)' : '#fff',
                            color: speedFilter === s ? 'var(--primary)' : 'var(--text-500)',
                            cursor: 'pointer', transition: '0.15s',
                        }}>
                        {s === 'ALL' ? 'All' : s === 'MOVING' ? '🟢 Moving' : '⏸ Stopped'}
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
                            : m.markerType === 'VEHICLE' ? m.vehicleName : m.driverName;
                        const sub = m.markerType === 'COMBINED'
                            ? `${m.tripOrigin} → ${m.tripDestination}`
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
                                    {m.markerType === 'COMBINED' && m.progressPercent != null && (
                                        <div style={{ marginTop: 4 }}>
                                            <div style={{
                                                height: 4, borderRadius: 2, background: '#e5e7eb',
                                                overflow: 'hidden', width: '100%',
                                            }}>
                                                <div style={{
                                                    width: `${Math.min(100, m.progressPercent)}%`,
                                                    height: '100%', borderRadius: 2,
                                                    background: 'linear-gradient(90deg, #27ae60, #8e44ad)',
                                                    transition: 'width 0.3s',
                                                }} />
                                            </div>
                                            <div style={{
                                                display: 'flex', justifyContent: 'space-between',
                                                fontSize: '0.65rem', color: 'var(--text-400)', marginTop: 2,
                                            }}>
                                                <span>{m.remainingDistanceKm?.toFixed(0)} km left</span>
                                                <span>ETA {formatETA(m.estimatedMinutesRemaining)}</span>
                                            </div>
                                        </div>
                                    )}
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

                        {/* Auto-center on driver's location if user is a driver */}
                        {user?.role === 'DRIVER' && (user?.driverId || user?.userId) && (
                            <AutoCenterOnDriver driverId={user?.driverId || user?.userId} markers={markers} />
                        )}

                        {/* Route polylines with fallback indicators */}
                        {routeLines.map(r => (
                            <span key={`route-${r.key}`}>
                                <Polyline positions={r.completed}
                                    pathOptions={{
                                        color: '#27ae60',
                                        weight: 5,
                                        opacity: r.isFallback ? 0.5 : 0.85,
                                        dashArray: r.isFallback ? '5, 10' : null
                                    }} />
                                <Polyline positions={r.remaining}
                                    pathOptions={{
                                        color: '#8e44ad',
                                        weight: 4,
                                        opacity: r.isFallback ? 0.4 : 0.6,
                                        dashArray: '10, 8'
                                    }} />
                                {r.isFallback && (
                                    <Marker position={r.originPos} icon={warningIcon}>
                                        <Popup>
                                            <div style={{ fontSize: '0.75rem', color: '#e67e22' }}>
                                                ⚠️ Approximate route (straight-line)<br />
                                                Road routing unavailable
                                            </div>
                                        </Popup>
                                    </Marker>
                                )}
                                <Marker position={r.originPos} icon={icons.ORIGIN}>
                                    <Popup><div style={{ fontFamily: 'Inter, sans-serif', fontSize: '0.78rem' }}>
                                        <b style={{ color: '#27ae60' }}>📍 Origin</b><br />{r.origin}
                                    </div></Popup>
                                </Marker>
                                <Marker position={r.destPos} icon={icons.DEST}>
                                    <Popup><div style={{ fontFamily: 'Inter, sans-serif', fontSize: '0.78rem' }}>
                                        <b style={{ color: '#c0392b' }}>🏁 Destination</b><br />{r.dest}
                                    </div></Popup>
                                </Marker>
                            </span>
                        ))}

                        {/* Fleet markers */}
                        {filtered.map(m => {
                            const route = showRoutes ? routeLines.find(r => r.key === markerKey(m)) : null;
                            const pos = route?.snappedPos || [m.latitude, m.longitude];
                            const currentDriverId = user?.driverId || user?.userId;
                            const isMe = (m.markerType === 'DRIVER' || m.markerType === 'COMBINED') && m.driverId === currentDriverId;

                            return (
                                <Marker key={markerKey(m)} position={pos}
                                    icon={getIcon(m, currentDriverId)}
                                    eventHandlers={{ click: () => setSelectedKey(markerKey(m)) }}>
                                    <Popup>
                                        <div style={{ fontFamily: 'Inter, sans-serif', minWidth: 200 }}>
                                            {isMe && (
                                                <div style={{
                                                    fontWeight: 700,
                                                    fontSize: '0.95rem',
                                                    color: '#e74c3c',
                                                    marginBottom: 8,
                                                    padding: '6px 10px',
                                                    background: '#ffebee',
                                                    borderRadius: 6,
                                                    textAlign: 'center'
                                                }}>
                                                    📍 You are here
                                                </div>
                                            )}
                                            {m.markerType === 'COMBINED' ? (
                                                <>
                                                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#8e44ad', marginBottom: 4 }}>
                                                        🟣 Active Trip
                                                    </div>
                                                    <div style={{ fontSize: '0.78rem', color: '#6b7280', lineHeight: 1.7 }}>
                                                        <div><b>Driver:</b> {m.driverName}</div>
                                                        <div><b>Vehicle:</b> {m.vehicleName} ({m.vehicleModel})</div>
                                                        <div><b>Plate:</b> {m.licensePlate}</div>
                                                        <div style={{ marginTop: 6, padding: '6px 8px', background: '#f3f0ff', borderRadius: 6 }}>
                                                            <div style={{ fontWeight: 600, color: '#8e44ad', marginBottom: 4, fontSize: '0.75rem' }}>
                                                                🛣️ Route
                                                            </div>
                                                            <div style={{ fontSize: '0.72rem' }}>
                                                                📍 {m.tripOrigin} → 🏁 {m.tripDestination}
                                                            </div>
                                                            <div style={{ margin: '6px 0', height: 5, borderRadius: 3, background: '#e5e7eb', overflow: 'hidden' }}>
                                                                <div style={{
                                                                    width: `${m.progressPercent || 0}%`, height: '100%', borderRadius: 3,
                                                                    background: 'linear-gradient(90deg, #27ae60, #8e44ad)',
                                                                }} />
                                                            </div>
                                                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem' }}>
                                                                <span>{m.progressPercent?.toFixed(0)}% complete</span>
                                                                <span>{m.totalDistanceKm?.toFixed(0)} km total</span>
                                                            </div>
                                                        </div>
                                                        <div style={{
                                                            marginTop: 6, display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4,
                                                            fontSize: '0.72rem', textAlign: 'center',
                                                        }}>
                                                            <div style={{ padding: '4px', background: '#eafaf1', borderRadius: 4 }}>
                                                                <div style={{ fontWeight: 700, color: '#27ae60' }}>{m.speed?.toFixed(0)}</div>
                                                                <div style={{ color: '#999' }}>km/h</div>
                                                            </div>
                                                            <div style={{ padding: '4px', background: '#fef5e7', borderRadius: 4 }}>
                                                                <div style={{ fontWeight: 700, color: '#d68910' }}>{m.remainingDistanceKm?.toFixed(0)}</div>
                                                                <div style={{ color: '#999' }}>km left</div>
                                                            </div>
                                                            <div style={{ padding: '4px', background: '#f5eef8', borderRadius: 4 }}>
                                                                <div style={{ fontWeight: 700, color: '#8e44ad' }}>{formatETA(m.estimatedMinutesRemaining)}</div>
                                                                <div style={{ color: '#999' }}>ETA</div>
                                                            </div>
                                                        </div>
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
                                                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: isMe ? '#e74c3c' : '#27ae60', marginBottom: 2 }}>
                                                        {isMe ? '🔴' : '🟢'} {m.driverName}
                                                    </div>
                                                    <div style={{ fontSize: '0.78rem', color: '#6b7280', lineHeight: 1.7 }}>
                                                        <div>License: {m.licenseNumber}</div>
                                                        <div>Category: {m.licenseCategory}</div>
                                                        <div>Status: {m.dutyStatus?.replace('_', ' ')}</div>
                                                        <div>Safety: {m.safetyScore}/100</div>
                                                        {m.speed > 0 && <div style={{ color: '#27ae60', fontWeight: 600 }}>Speed: {m.speed?.toFixed(1)} km/h</div>}
                                                        {m.accuracy != null && (
                                                            <div style={{ marginTop: 6, fontSize: '0.75rem' }}>
                                                                <div style={{ color: '#6b7280' }}>
                                                                    Accuracy: ±{m.accuracy.toFixed(0)}m
                                                                </div>
                                                                {m.accuracy > 100 && (
                                                                    <div style={{
                                                                        marginTop: 4,
                                                                        padding: '4px 8px',
                                                                        background: '#fff3cd',
                                                                        borderRadius: 4,
                                                                        fontSize: '0.7rem',
                                                                        color: '#856404'
                                                                    }}>
                                                                        ⚠️ Low accuracy warning
                                                                    </div>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                </>
                                            )}
                                            <div style={{ fontSize: '0.7rem', color: '#9ca3af', marginTop: 4 }}>
                                                Updated: {new Date(m.lastUpdated).toLocaleTimeString()}
                                            </div>
                                        </div>
                                    </Popup>
                                </Marker>
                            );
                        })}
                    </MapContainer>
                </div>
            </div>
        </div>
    );
}
