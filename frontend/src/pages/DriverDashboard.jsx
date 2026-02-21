import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { locationAPI } from '../api/locationAPI';
import { tripAPI } from '../api';
import geolocationService from '../services/GeolocationService';
import { MapPin, Navigation, Play, Square, RefreshCw, Clock, TrendingUp, CheckCircle, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * DriverDashboard Component
 * 
 * Driver-specific dashboard with location sharing controls, trip summary, and statistics.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 2.1, 2.2, 2.4, 3.5
 */

export default function DriverDashboard() {
    const { user } = useAuth();
    const navigate = useNavigate();

    // Location sharing state
    const [locationSharing, setLocationSharing] = useState(false);
    const [currentLocation, setCurrentLocation] = useState(null);
    const [lastUpdate, setLastUpdate] = useState(null);
    const [permissionStatus, setPermissionStatus] = useState('prompt');
    const [isUpdating, setIsUpdating] = useState(false);

    // Trip and stats state
    const [todayTrips, setTodayTrips] = useState([]);
    const [stats, setStats] = useState({ totalTrips: 0, totalDistance: 0 });
    const [loading, setLoading] = useState(true);

    // Check geolocation permission on mount
    useEffect(() => {
        checkPermission();
        loadDashboardData();
    }, []);

    const checkPermission = async () => {
        const status = await geolocationService.checkPermission();
        setPermissionStatus(status);
    };

    const loadDashboardData = async () => {
        try {
            // Load today's trips
            const tripsRes = await tripAPI.getAll();
            const allTrips = tripsRes.data.data?.content || [];

            // Filter to today's active or recent trips for this driver
            const today = new Date().toISOString().split('T')[0];
            const driverTrips = allTrips.filter(trip =>
                trip.driverId === (user.driverId || user.userId) &&
                (trip.status === 'DISPATCHED' || trip.status === 'DRAFT' ||
                    (trip.createdAt && trip.createdAt.startsWith(today)))
            );
            setTodayTrips(driverTrips);

            // Calculate stats (completed trips and total distance)
            const completedTrips = allTrips.filter(trip =>
                trip.driverId === (user.driverId || user.userId) &&
                trip.status === 'COMPLETED'
            );

            const totalDistance = completedTrips.reduce((sum, trip) =>
                sum + (trip.distanceKm || 0), 0
            );

            setStats({
                totalTrips: completedTrips.length,
                totalDistance: totalDistance
            });

            // Try to load current location
            try {
                const locationRes = await locationAPI.getDriverLocation(user.userId);
                if (locationRes.data.data) {
                    const loc = locationRes.data.data;
                    setCurrentLocation({
                        latitude: loc.latitude,
                        longitude: loc.longitude,
                        accuracy: loc.accuracy
                    });
                    setLocationSharing(loc.sharingActive || false);
                    setLastUpdate(new Date(loc.lastUpdated));
                }
            } catch (err) {
                // Location not found yet - that's okay
                console.log('No location data yet');
            }
        } catch (err) {
            toast.error('Failed to load dashboard data');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleLocationSuccess = useCallback(async (position) => {
        const locationData = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            speed: position.coords.speed || 0,
            heading: position.coords.heading || 0
        };

        setCurrentLocation(locationData);
        setLastUpdate(new Date());

        try {
            if (locationSharing) {
                // Update location on server
                await locationAPI.updateLocation(locationData);
            }
        } catch (err) {
            console.error('Failed to update location:', err);
            // Don't show error toast for every update failure
        }
    }, [locationSharing]);

    const handleLocationError = useCallback((error) => {
        console.error('Location error:', error);

        if (error.type === 'PERMISSION_DENIED') {
            setPermissionStatus('denied');
            toast.error(error.message, { duration: 5000 });
        } else if (error.type === 'AUTO_STOP') {
            setLocationSharing(false);
            toast.error(error.message, { duration: 6000 });
        } else if (!error.retry) {
            toast.error(error.message);
        }
    }, []);

    const startLocationSharing = async () => {
        if (permissionStatus === 'denied') {
            toast.error('Location permission denied. Please enable it in your browser settings.');
            return;
        }

        setIsUpdating(true);

        try {
            // Get initial position
            geolocationService.getCurrentPosition(
                async (position) => {
                    const locationData = {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                        accuracy: position.coords.accuracy,
                        speed: position.coords.speed || 0,
                        heading: position.coords.heading || 0
                    };

                    // Start sharing on server
                    await locationAPI.startLocationSharing(locationData);

                    setCurrentLocation(locationData);
                    setLastUpdate(new Date());
                    setLocationSharing(true);

                    // Start continuous tracking
                    geolocationService.startTracking(handleLocationSuccess, handleLocationError);

                    toast.success('Location sharing started');
                    setIsUpdating(false);
                },
                (error) => {
                    handleLocationError(error);
                    setIsUpdating(false);
                }
            );
        } catch (err) {
            toast.error('Failed to start location sharing');
            console.error(err);
            setIsUpdating(false);
        }
    };

    const stopLocationSharing = async () => {
        setIsUpdating(true);

        try {
            // Stop tracking
            geolocationService.stopTracking();

            // Stop sharing on server
            await locationAPI.stopLocationSharing();

            setLocationSharing(false);
            toast.success('Location sharing stopped');
        } catch (err) {
            toast.error('Failed to stop location sharing');
            console.error(err);
        } finally {
            setIsUpdating(false);
        }
    };

    const updateLocationManually = async () => {
        if (!locationSharing) {
            toast.error('Please start location sharing first');
            return;
        }

        setIsUpdating(true);

        geolocationService.getCurrentPosition(
            async (position) => {
                const locationData = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                    speed: position.coords.speed || 0,
                    heading: position.coords.heading || 0
                };

                try {
                    await locationAPI.updateLocation(locationData);
                    setCurrentLocation(locationData);
                    setLastUpdate(new Date());
                    toast.success('Location updated');
                } catch (err) {
                    toast.error('Failed to update location');
                    console.error(err);
                } finally {
                    setIsUpdating(false);
                }
            },
            (error) => {
                handleLocationError(error);
                setIsUpdating(false);
            }
        );
    };

    const viewOnMap = () => {
        navigate('/live-map');
    };

    if (loading) {
        return <div className="loading-spinner"><div className="spinner" /></div>;
    }

    return (
        <div>
            {/* Location Sharing Section */}
            <div className="card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <MapPin size={20} color="var(--primary)" />
                        <h3 style={{ margin: 0 }}>Location Sharing</h3>
                    </div>
                    <div style={{
                        padding: '4px 12px',
                        borderRadius: 12,
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        background: locationSharing ? '#eafaf1' : '#f5f5f5',
                        color: locationSharing ? '#27ae60' : '#7f8c8d'
                    }}>
                        {locationSharing ? '🟢 Active' : '⚫ Inactive'}
                    </div>
                </div>

                {/* Current Location Display */}
                {currentLocation && (
                    <div style={{
                        background: '#f8f9fa',
                        padding: 12,
                        borderRadius: 8,
                        marginBottom: 12,
                        fontSize: '0.85rem'
                    }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                            <div>
                                <div style={{ color: 'var(--text-400)', fontSize: '0.75rem' }}>Latitude</div>
                                <div style={{ fontWeight: 600, fontFamily: 'monospace' }}>
                                    {currentLocation.latitude.toFixed(6)}
                                </div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-400)', fontSize: '0.75rem' }}>Longitude</div>
                                <div style={{ fontWeight: 600, fontFamily: 'monospace' }}>
                                    {currentLocation.longitude.toFixed(6)}
                                </div>
                            </div>
                        </div>
                        {currentLocation.accuracy && (
                            <div style={{ marginTop: 8, fontSize: '0.75rem', color: 'var(--text-500)' }}>
                                Accuracy: ±{currentLocation.accuracy.toFixed(0)}m
                                {currentLocation.accuracy > 100 && (
                                    <span style={{ color: '#d68910', marginLeft: 8 }}>⚠️ Low accuracy</span>
                                )}
                            </div>
                        )}
                        {lastUpdate && (
                            <div style={{ marginTop: 4, fontSize: '0.75rem', color: 'var(--text-400)' }}>
                                Last updated: {lastUpdate.toLocaleTimeString()}
                            </div>
                        )}
                    </div>
                )}

                {/* Control Buttons */}
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {!locationSharing ? (
                        <button
                            onClick={startLocationSharing}
                            disabled={isUpdating || permissionStatus === 'denied'}
                            className="btn btn-primary"
                            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                        >
                            <Play size={16} />
                            Start Location Sharing
                        </button>
                    ) : (
                        <button
                            onClick={stopLocationSharing}
                            disabled={isUpdating}
                            className="btn"
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 6,
                                background: '#c0392b',
                                color: '#fff'
                            }}
                        >
                            <Square size={16} />
                            Stop Sharing
                        </button>
                    )}

                    <button
                        onClick={updateLocationManually}
                        disabled={isUpdating || !locationSharing}
                        className="btn"
                        style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                    >
                        <RefreshCw size={16} />
                        Update My Location
                    </button>

                    <button
                        onClick={viewOnMap}
                        className="btn"
                        style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                    >
                        <MapPin size={16} />
                        View on Map
                    </button>
                </div>

                {permissionStatus === 'denied' && (
                    <div style={{
                        marginTop: 12,
                        padding: 12,
                        background: '#fff3cd',
                        border: '1px solid #ffc107',
                        borderRadius: 8,
                        fontSize: '0.85rem',
                        color: '#856404'
                    }}>
                        <AlertCircle size={16} style={{ marginRight: 6, verticalAlign: 'middle' }} />
                        Location permission denied. Please enable location access in your browser settings to use this feature.
                    </div>
                )}
            </div>

            {/* Today's Trips Section */}
            <div className="card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                    <Navigation size={20} color="var(--primary)" />
                    <h3 style={{ margin: 0 }}>Today's Trips</h3>
                    <span style={{
                        padding: '2px 8px',
                        borderRadius: 12,
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        background: '#e3f2fd',
                        color: '#2980b9'
                    }}>
                        {todayTrips.length}
                    </span>
                </div>

                {todayTrips.length === 0 ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '40px 20px',
                        color: 'var(--text-400)',
                        fontSize: '0.9rem'
                    }}>
                        No trips assigned for today
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {todayTrips.map(trip => (
                            <div
                                key={trip.id}
                                style={{
                                    padding: 12,
                                    border: '1px solid var(--border)',
                                    borderRadius: 8,
                                    cursor: 'pointer',
                                    transition: '0.2s'
                                }}
                                onClick={() => navigate(`/trips`)}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 8 }}>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontWeight: 600, fontSize: '0.9rem', marginBottom: 4 }}>
                                            {trip.originName || trip.origin} → {trip.destinationName || trip.destination}
                                        </div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--text-500)' }}>
                                            {trip.vehicleName} • {trip.distanceKm?.toFixed(0)} km
                                        </div>
                                        <a
                                            href={`https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(trip.origin)}&destination=${encodeURIComponent(trip.destination)}`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            onClick={(e) => e.stopPropagation()}
                                            style={{
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                fontSize: '0.7rem',
                                                color: '#2980b9',
                                                textDecoration: 'none',
                                                marginTop: 8,
                                                background: '#e3f2fd',
                                                padding: '4px 10px',
                                                borderRadius: 12,
                                                fontWeight: 600
                                            }}
                                        >
                                            🗺️ Open in Google Maps
                                        </a>
                                    </div>
                                    <div style={{
                                        padding: '4px 10px',
                                        borderRadius: 12,
                                        fontSize: '0.7rem',
                                        fontWeight: 600,
                                        background: trip.status === 'COMPLETED' ? '#eafaf1' :
                                            trip.status === 'IN_PROGRESS' ? '#e3f2fd' :
                                                trip.status === 'SCHEDULED' ? '#fff3cd' : '#f5f5f5',
                                        color: trip.status === 'COMPLETED' ? '#27ae60' :
                                            trip.status === 'IN_PROGRESS' ? '#2980b9' :
                                                trip.status === 'SCHEDULED' ? '#d68910' : '#7f8c8d'
                                    }}>
                                        {trip.status.replace('_', ' ')}
                                    </div>
                                </div>
                                {trip.createdAt && (
                                    <div style={{ fontSize: '0.75rem', color: 'var(--text-400)', display: 'flex', alignItems: 'center', gap: 4 }}>
                                        <Clock size={12} />
                                        {new Date(trip.createdAt).toLocaleString()}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Statistics Section */}
            <div className="grid-2">
                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                        <CheckCircle size={20} color="#27ae60" />
                        <h3 style={{ margin: 0 }}>Total Trips Completed</h3>
                    </div>
                    <div style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {stats.totalTrips}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-400)', marginTop: 4 }}>
                        All-time completed trips
                    </div>
                </div>

                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                        <TrendingUp size={20} color="#2980b9" />
                        <h3 style={{ margin: 0 }}>Total Distance Driven</h3>
                    </div>
                    <div style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {stats.totalDistance.toFixed(0)}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-400)', marginTop: 4 }}>
                        Kilometers driven
                    </div>
                </div>
            </div>
        </div>
    );
}
