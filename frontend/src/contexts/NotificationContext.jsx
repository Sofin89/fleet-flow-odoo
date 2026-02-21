import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { useNavigate } from 'react-router-dom';
import webSocketService from '../services/WebSocketService';
import toast from 'react-hot-toast';

/**
 * NotificationContext
 * 
 * Manages trip assignment notifications with offline queue support.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */

const NotificationContext = createContext();

const NOTIFICATION_STORAGE_KEY = 'fleetflow_notifications';

export function NotificationProvider({ children }) {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const loadQueuedNotifications = () => {
        try {
            const stored = localStorage.getItem(NOTIFICATION_STORAGE_KEY);
            if (stored) {
                const queued = JSON.parse(stored);
                if (queued.length > 0) {
                    setNotifications(queued);
                    setUnreadCount(queued.filter(n => !n.read).length);
                    
                    // Show toast for each queued notification
                    queued.forEach(notification => {
                        if (!notification.read) {
                            showNotificationToast(notification);
                        }
                    });
                }
            }
        } catch (error) {
            console.error('Failed to load queued notifications:', error);
        }
    };

    const saveNotificationsToStorage = (notifs) => {
        try {
            localStorage.setItem(NOTIFICATION_STORAGE_KEY, JSON.stringify(notifs));
        } catch (error) {
            console.error('Failed to save notifications:', error);
        }
    };

    const handleTripAssignment = useCallback((event) => {
        const notification = {
            id: event.tripId || Date.now(),
            tripId: event.tripId,
            type: 'TRIP_ASSIGNMENT',
            title: 'New Trip Assigned',
            message: `${event.origin} → ${event.destination}`,
            origin: event.origin,
            destination: event.destination,
            scheduledDate: event.scheduledDate,
            vehicleName: event.vehicleName,
            timestamp: new Date().toISOString(),
            read: false
        };

        // Add to notifications list
        setNotifications(prev => {
            const updated = [notification, ...prev];
            saveNotificationsToStorage(updated);
            return updated;
        });

        setUnreadCount(prev => prev + 1);

        // Show toast notification
        showNotificationToast(notification);
    }, []);

    const showNotificationToast = (notification) => {
        toast.custom((t) => (
            <div
                onClick={() => {
                    handleNotificationClick(notification);
                    toast.dismiss(t.id);
                }}
                style={{
                    background: '#fff',
                    padding: '16px',
                    borderRadius: '8px',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    border: '2px solid #2980b9',
                    cursor: 'pointer',
                    maxWidth: '400px',
                    transition: '0.2s'
                }}
            >
                <div style={{ display: 'flex', alignItems: 'start', gap: 12 }}>
                    <div style={{
                        width: 40,
                        height: 40,
                        borderRadius: '50%',
                        background: '#e3f2fd',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '1.2rem',
                        flexShrink: 0
                    }}>
                        🚚
                    </div>
                    <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#2980b9', marginBottom: 4 }}>
                            {notification.title}
                        </div>
                        <div style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: 6 }}>
                            {notification.message}
                        </div>
                        {notification.vehicleName && (
                            <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
                                Vehicle: {notification.vehicleName}
                            </div>
                        )}
                        <div style={{
                            marginTop: 8,
                            fontSize: '0.75rem',
                            color: '#2980b9',
                            fontWeight: 600
                        }}>
                            Click to view details →
                        </div>
                    </div>
                </div>
            </div>
        ), {
            duration: 8000,
            position: 'top-right'
        });
    };

    const handleNotificationClick = (notification) => {
        // Mark as read
        markAsRead(notification.id);
        
        // Navigate to trips page
        navigate('/trips');
    };

    const markAsRead = (notificationId) => {
        setNotifications(prev => {
            const updated = prev.map(n => 
                n.id === notificationId ? { ...n, read: true } : n
            );
            saveNotificationsToStorage(updated);
            return updated;
        });

        setUnreadCount(prev => Math.max(0, prev - 1));
    };

    const markAllAsRead = () => {
        setNotifications(prev => {
            const updated = prev.map(n => ({ ...n, read: true }));
            saveNotificationsToStorage(updated);
            return updated;
        });

        setUnreadCount(0);
    };

    const clearNotifications = () => {
        setNotifications([]);
        setUnreadCount(0);
        localStorage.removeItem(NOTIFICATION_STORAGE_KEY);
    };

    // Load queued notifications from localStorage on mount
    useEffect(() => {
        if (user?.role === 'DRIVER') {
            loadQueuedNotifications();
        }
    }, [user]);

    // Connect to WebSocket for trip assignments
    useEffect(() => {
        if (!user || user.role !== 'DRIVER') return;

        let tripSub = null;

        webSocketService.connect(
            () => {
                console.log('WebSocket connected for notifications');
                
                // Subscribe to trip assignments for this driver
                tripSub = webSocketService.subscribe(
                    `/topic/trip-assignments/${user.userId}`,
                    handleTripAssignment
                );
            },
            (error) => {
                console.error('WebSocket connection error for notifications:', error);
            }
        );

        return () => {
            if (tripSub) {
                webSocketService.unsubscribe(tripSub);
            }
            // Don't disconnect here as other components may be using it
        };
    }, [user, handleTripAssignment]);

    const value = {
        notifications,
        unreadCount,
        markAsRead,
        markAllAsRead,
        clearNotifications
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotifications() {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within NotificationProvider');
    }
    return context;
}
