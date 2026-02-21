import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { locationAPI } from '../api/locationAPI';
import { tripAPI } from '../api';
import { User, MapPin, Navigation, Calendar, Clock, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';

/**
 * DriverProfile Component
 * 
 * Driver profile page with trip history, location sharing statistics, and location history timeline.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */

export default function DriverProfile() {
    const { user } = useAuth();
    const [completedTrips, setCompletedTrips] = useState([]);
    const [locationHistory, setLocationHistory] = useState([]);
    const [stats, setStats] = useState({
        totalTrips: 0,
        totalDistance: 0,
        totalHoursTracked: 0
    });
    const [loading, setLoading] = useState(true);
    const [historyLoading, setHistoryLoading] = useState(false);
    
    // Date range for location history (default: today)
    const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
    const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
    const [dateRangeError, setDateRangeError] = useState('');

    useEffect(() => {
        loadProfileData();
        loadLocationHistory();
    }, []);

    const loadProfileData = async () => {
        try {
            // Load completed trips
            const tripsRes = await tripAPI.getAll();
            const allTrips = tripsRes.data.data?.content || [];
            
            const driverCompletedTrips = allTrips.filter(trip => 
                trip.driverId === user.userId && trip.status === 'COMPLETED'
            );
            
            setCompletedTrips(driverCompletedTrips);

            // Calculate statistics
            const totalDistance = driverCompletedTrips.reduce((sum, trip) => 
                sum + (trip.distanceKm || 0), 0
            );

            // Estimate hours tracked (rough estimate based on trips)
            const totalHoursTracked = driverCompletedTrips.reduce((sum, trip) => {
                // Estimate 1 hour per 60 km
                return sum + ((trip.distanceKm || 0) / 60);
            }, 0);

            setStats({
                totalTrips: driverCompletedTrips.length,
                totalDistance: totalDistance,
                totalHoursTracked: totalHoursTracked
            });
        } catch (err) {
            toast.error('Failed to load profile data');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const loadLocationHistory = async () => {
        setHistoryLoading(true);
        setDateRangeError('');

        try {
            // Validate date range (max 30 days)
            const start = new Date(startDate);
            const end = new Date(endDate);
            const daysDiff = Math.ceil((end - start) / (1000 * 60 * 60 * 24));

            if (daysDiff > 30) {
                setDateRangeError('Date range cannot exceed 30 days');
                setHistoryLoading(false);
                return;
            }

            if (daysDiff < 0) {
                setDateRangeError('End date must be after start date');
                setHistoryLoading(false);
                return;
            }

            const historyRes = await locationAPI.getLocationHistory(
                user.userId,
                startDate,
                endDate
            );

            setLocationHistory(historyRes.data.data || []);
        } catch (err) {
            if (err.response?.status === 400) {
                setDateRangeError('Invalid date range. Maximum 30 days allowed.');
            } else {
                toast.error('Failed to load location history');
            }
            console.error(err);
        } finally {
            setHistoryLoading(false);
        }
    };

    const handleDateRangeChange = () => {
        loadLocationHistory();
    };

    if (loading) {
        return <div className="loading-spinner"><div className="spinner" /></div>;
    }

    return (
        <div>
            {/* Profile Header */}
            <div className="card" style={{ marginBottom: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{
                        width: 80,
                        height: 80,
                        borderRadius: '50%',
                        background: 'linear-gradient(135deg, #2980b9, #27ae60)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '2rem',
                        color: '#fff',
                        fontWeight: 700
                    }}>
                        {user.fullName?.charAt(0).toUpperCase()}
                    </div>
                    <div style={{ flex: 1 }}>
                        <h2 style={{ margin: 0, marginBottom: 4 }}>{user.fullName}</h2>
                        <div style={{ fontSize: '0.9rem', color: 'var(--text-500)', marginBottom: 4 }}>
                            <User size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                            Driver
                        </div>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-400)' }}>
                            {user.email}
                        </div>
                    </div>
                </div>
            </div>

            {/* Statistics Cards */}
            <div className="grid-3" style={{ marginBottom: 20 }}>
                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                        <Navigation size={18} color="#2980b9" />
                        <h4 style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-500)' }}>
                            Completed Trips
                        </h4>
                    </div>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {stats.totalTrips}
                    </div>
                </div>

                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                        <TrendingUp size={18} color="#27ae60" />
                        <h4 style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-500)' }}>
                            Total Distance
                        </h4>
                    </div>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {stats.totalDistance.toFixed(0)}
                        <span style={{ fontSize: '1rem', fontWeight: 400, color: 'var(--text-400)', marginLeft: 4 }}>
                            km
                        </span>
                    </div>
                </div>

                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                        <Clock size={18} color="#8e44ad" />
                        <h4 style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-500)' }}>
                            Hours Tracked
                        </h4>
                    </div>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {stats.totalHoursTracked.toFixed(1)}
                        <span style={{ fontSize: '1rem', fontWeight: 400, color: 'var(--text-400)', marginLeft: 4 }}>
                            hrs
                        </span>
                    </div>
                </div>
            </div>

            {/* Completed Trips List */}
            <div className="card" style={{ marginBottom: 20 }}>
                <h3 style={{ marginBottom: 16 }}>Completed Trips</h3>
                {completedTrips.length === 0 ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '40px 20px',
                        color: 'var(--text-400)',
                        fontSize: '0.9rem'
                    }}>
                        No completed trips yet
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {completedTrips.slice(0, 10).map(trip => (
                            <div
                                key={trip.id}
                                style={{
                                    padding: 12,
                                    border: '1px solid var(--border)',
                                    borderRadius: 8,
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center'
                                }}
                            >
                                <div style={{ flex: 1 }}>
                                    <div style={{ fontWeight: 600, fontSize: '0.9rem', marginBottom: 4 }}>
                                        {trip.origin} → {trip.destination}
                                    </div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-500)' }}>
                                        {trip.vehicleName} • {trip.distanceKm?.toFixed(0)} km
                                    </div>
                                    {trip.scheduledDate && (
                                        <div style={{ fontSize: '0.75rem', color: 'var(--text-400)', marginTop: 4 }}>
                                            <Calendar size={12} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                                            {new Date(trip.scheduledDate).toLocaleDateString()}
                                        </div>
                                    )}
                                </div>
                                <div style={{
                                    padding: '4px 10px',
                                    borderRadius: 12,
                                    fontSize: '0.7rem',
                                    fontWeight: 600,
                                    background: '#eafaf1',
                                    color: '#27ae60'
                                }}>
                                    COMPLETED
                                </div>
                            </div>
                        ))}
                        {completedTrips.length > 10 && (
                            <div style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-400)' }}>
                                Showing 10 of {completedTrips.length} trips
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Location History Timeline */}
            <div className="card">
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                    <MapPin size={20} color="var(--primary)" />
                    <h3 style={{ margin: 0 }}>Location History</h3>
                </div>

                {/* Date Range Selector */}
                <div style={{ marginBottom: 16 }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'end', flexWrap: 'wrap' }}>
                        <div style={{ flex: 1, minWidth: 200 }}>
                            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: 4, color: 'var(--text-600)' }}>
                                Start Date
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                max={new Date().toISOString().split('T')[0]}
                                style={{
                                    width: '100%',
                                    padding: '8px 12px',
                                    borderRadius: 6,
                                    border: '1px solid var(--border)',
                                    fontSize: '0.85rem'
                                }}
                            />
                        </div>
                        <div style={{ flex: 1, minWidth: 200 }}>
                            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: 4, color: 'var(--text-600)' }}>
                                End Date
                            </label>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                max={new Date().toISOString().split('T')[0]}
                                style={{
                                    width: '100%',
                                    padding: '8px 12px',
                                    borderRadius: 6,
                                    border: '1px solid var(--border)',
                                    fontSize: '0.85rem'
                                }}
                            />
                        </div>
                        <button
                            onClick={handleDateRangeChange}
                            disabled={historyLoading}
                            className="btn btn-primary"
                            style={{ padding: '8px 20px' }}
                        >
                            {historyLoading ? 'Loading...' : 'Load History'}
                        </button>
                    </div>
                    {dateRangeError && (
                        <div style={{
                            marginTop: 8,
                            padding: 8,
                            background: '#fff3cd',
                            border: '1px solid #ffc107',
                            borderRadius: 6,
                            fontSize: '0.85rem',
                            color: '#856404'
                        }}>
                            {dateRangeError}
                        </div>
                    )}
                    <div style={{ marginTop: 8, fontSize: '0.75rem', color: 'var(--text-400)' }}>
                        Maximum date range: 30 days
                    </div>
                </div>

                {/* Location History Timeline */}
                {historyLoading ? (
                    <div style={{ textAlign: 'center', padding: '40px 20px' }}>
                        <div className="spinner" style={{ margin: '0 auto' }} />
                    </div>
                ) : locationHistory.length === 0 ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '40px 20px',
                        color: 'var(--text-400)',
                        fontSize: '0.9rem'
                    }}>
                        No location history for selected date range
                    </div>
                ) : (
                    <div style={{ maxHeight: 400, overflowY: 'auto' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                            {locationHistory.map((loc, index) => (
                                <div
                                    key={loc.id}
                                    style={{
                                        padding: 10,
                                        background: index === 0 ? '#f0f9ff' : '#f8f9fa',
                                        borderRadius: 6,
                                        borderLeft: index === 0 ? '3px solid #2980b9' : '3px solid #e5e7eb',
                                        fontSize: '0.85rem'
                                    }}
                                >
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                                        <div style={{ flex: 1 }}>
                                            <div style={{ fontWeight: 600, marginBottom: 4, fontFamily: 'monospace' }}>
                                                {loc.latitude.toFixed(6)}, {loc.longitude.toFixed(6)}
                                            </div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-500)' }}>
                                                {new Date(loc.recordedAt).toLocaleString()}
                                            </div>
                                            {loc.accuracy && (
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-400)', marginTop: 2 }}>
                                                    Accuracy: ±{loc.accuracy.toFixed(0)}m
                                                    {loc.accuracy > 100 && (
                                                        <span style={{ color: '#d68910', marginLeft: 6 }}>⚠️</span>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                        {loc.speed > 0 && (
                                            <div style={{
                                                padding: '4px 8px',
                                                borderRadius: 12,
                                                fontSize: '0.7rem',
                                                fontWeight: 600,
                                                background: '#eafaf1',
                                                color: '#27ae60'
                                            }}>
                                                {loc.speed.toFixed(0)} km/h
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
