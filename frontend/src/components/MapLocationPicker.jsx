import { useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { X, Check } from 'lucide-react';

// Custom icons for origin and destination
const originIcon = L.divIcon({
    className: '',
    html: '<div style="font-size:24px;text-shadow:0 1px 3px rgba(0,0,0,0.4);">📍</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 24],
});

const destIcon = L.divIcon({
    className: '',
    html: '<div style="font-size:24px;text-shadow:0 1px 3px rgba(0,0,0,0.4);">🏁</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 24],
});

// Component to handle map clicks
function MapClickHandler({ onOriginClick, onDestClick, selectingOrigin }) {
    useMapEvents({
        click: (e) => {
            const { lat, lng } = e.latlng;
            if (selectingOrigin) {
                onOriginClick(lat, lng);
            } else {
                onDestClick(lat, lng);
            }
        },
    });
    return null;
}

export default function MapLocationPicker({ onConfirm, onCancel }) {
    const [origin, setOrigin] = useState(null);
    const [destination, setDestination] = useState(null);
    const [originName, setOriginName] = useState('');
    const [destName, setDestName] = useState('');
    const [selectingOrigin, setSelectingOrigin] = useState(true);

    const handleOriginClick = (lat, lng) => {
        setOrigin({ lat, lng });
        setSelectingOrigin(false);
    };

    const handleDestClick = (lat, lng) => {
        setDestination({ lat, lng });
    };

    const handleConfirm = () => {
        if (!origin || !destination) {
            alert('Please select both origin and destination');
            return;
        }
        onConfirm({
            origin: `${origin.lat},${origin.lng}`,
            destination: `${destination.lat},${destination.lng}`,
            originName: originName || null,
            destinationName: destName || null,
        });
    };

    return (
        <div className="modal-overlay">
            <div className="modal" style={{ maxWidth: '90vw', width: 900, maxHeight: '90vh' }}>
                <div className="modal-header">
                    <h3>Select Trip Locations</h3>
                    <button className="modal-close" onClick={onCancel}>
                        <X size={20} />
                    </button>
                </div>
                <div className="modal-body" style={{ padding: 0 }}>
                    <div style={{ padding: 16, background: '#f8f9fa', borderBottom: '1px solid var(--border)' }}>
                        <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
                            <button 
                                onClick={() => setSelectingOrigin(true)}
                                style={{
                                    padding: '8px 16px',
                                    borderRadius: 6,
                                    border: selectingOrigin ? '2px solid #27ae60' : '1px solid var(--border)',
                                    background: selectingOrigin ? '#eafaf1' : '#fff',
                                    color: selectingOrigin ? '#27ae60' : 'var(--text-600)',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                }}>
                                📍 Select Origin {origin && '✓'}
                            </button>
                            <button 
                                onClick={() => setSelectingOrigin(false)}
                                style={{
                                    padding: '8px 16px',
                                    borderRadius: 6,
                                    border: !selectingOrigin ? '2px solid #c0392b' : '1px solid var(--border)',
                                    background: !selectingOrigin ? '#fef5e7' : '#fff',
                                    color: !selectingOrigin ? '#c0392b' : 'var(--text-600)',
                                    fontWeight: 600,
                                    cursor: 'pointer',
                                }}>
                                🏁 Select Destination {destination && '✓'}
                            </button>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                            <div>
                                <label style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: 4, display: 'block' }}>
                                    Origin Name (optional)
                                </label>
                                <input 
                                    type="text"
                                    className="form-input"
                                    placeholder="e.g., Warehouse A"
                                    value={originName}
                                    onChange={(e) => setOriginName(e.target.value)}
                                    disabled={!origin}
                                />
                            </div>
                            <div>
                                <label style={{ fontSize: '0.75rem', fontWeight: 600, marginBottom: 4, display: 'block' }}>
                                    Destination Name (optional)
                                </label>
                                <input 
                                    type="text"
                                    className="form-input"
                                    placeholder="e.g., Customer Site"
                                    value={destName}
                                    onChange={(e) => setDestName(e.target.value)}
                                    disabled={!destination}
                                />
                            </div>
                        </div>
                    </div>
                    <MapContainer 
                        center={[23.0225, 72.5714]} 
                        zoom={12} 
                        style={{ height: 500, width: '100%' }}>
                        <TileLayer
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                            attribution='&copy; OpenStreetMap contributors'
                        />
                        <MapClickHandler 
                            onOriginClick={handleOriginClick}
                            onDestClick={handleDestClick}
                            selectingOrigin={selectingOrigin}
                        />
                        {destination && (
                            <Marker position={[destination.lat, destination.lng]} icon={destIcon} />
                        )}
                    </MapContainer>
                </div>
                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={onCancel}>
                        Cancel
                    </button>
                    <button 
                        className="btn btn-primary" 
                        onClick={handleConfirm}
                        disabled={!origin || !destination}>
                        <Check size={16} /> Confirm Locations
                    </button>
                </div>
            </div>
        </div>
    );
}
