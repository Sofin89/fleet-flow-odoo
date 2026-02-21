import { X, MapPin, AlertCircle } from 'lucide-react';

/**
 * PermissionModal Component
 * 
 * Modal that explains why location access is needed and provides instructions
 * for enabling geolocation permissions in different browsers.
 * 
 * Requirements: 12.1, 12.2
 */

export default function PermissionModal({ isOpen, onClose }) {
    if (!isOpen) return null;

    const getBrowserInstructions = () => {
        const userAgent = navigator.userAgent.toLowerCase();
        
        if (userAgent.includes('chrome') && !userAgent.includes('edg')) {
            return {
                browser: 'Google Chrome',
                steps: [
                    'Click the lock icon (🔒) in the address bar',
                    'Find "Location" in the permissions list',
                    'Change it from "Block" to "Allow"',
                    'Refresh the page'
                ]
            };
        } else if (userAgent.includes('firefox')) {
            return {
                browser: 'Mozilla Firefox',
                steps: [
                    'Click the lock icon (🔒) in the address bar',
                    'Click "Connection secure" or "Connection not secure"',
                    'Click "More Information"',
                    'Go to the "Permissions" tab',
                    'Find "Access Your Location" and uncheck "Use Default"',
                    'Select "Allow"',
                    'Refresh the page'
                ]
            };
        } else if (userAgent.includes('safari')) {
            return {
                browser: 'Safari',
                steps: [
                    'Open Safari Preferences (Safari > Preferences)',
                    'Go to the "Websites" tab',
                    'Select "Location" from the left sidebar',
                    'Find this website in the list',
                    'Change the permission to "Allow"',
                    'Refresh the page'
                ]
            };
        } else if (userAgent.includes('edg')) {
            return {
                browser: 'Microsoft Edge',
                steps: [
                    'Click the lock icon (🔒) in the address bar',
                    'Find "Location" in the permissions list',
                    'Change it from "Block" to "Allow"',
                    'Refresh the page'
                ]
            };
        } else {
            return {
                browser: 'Your Browser',
                steps: [
                    'Look for a location icon or lock icon in the address bar',
                    'Click it to open site permissions',
                    'Find the location permission setting',
                    'Change it to "Allow"',
                    'Refresh the page'
                ]
            };
        }
    };

    const instructions = getBrowserInstructions();

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 20
        }}>
            <div style={{
                background: '#fff',
                borderRadius: 12,
                maxWidth: 500,
                width: '100%',
                maxHeight: '90vh',
                overflow: 'auto',
                boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)'
            }}>
                {/* Header */}
                <div style={{
                    padding: '20px 24px',
                    borderBottom: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        <div style={{
                            width: 40,
                            height: 40,
                            borderRadius: '50%',
                            background: '#e3f2fd',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}>
                            <MapPin size={20} color="#2980b9" />
                        </div>
                        <h3 style={{ margin: 0 }}>Location Access Required</h3>
                    </div>
                    <button
                        onClick={onClose}
                        style={{
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            padding: 8,
                            borderRadius: 6,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: 'var(--text-400)',
                            transition: '0.2s'
                        }}
                        onMouseEnter={(e) => e.target.style.background = '#f5f5f5'}
                        onMouseLeave={(e) => e.target.style.background = 'none'}
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div style={{ padding: 24 }}>
                    {/* Why we need location */}
                    <div style={{
                        padding: 16,
                        background: '#fff3cd',
                        border: '1px solid #ffc107',
                        borderRadius: 8,
                        marginBottom: 20
                    }}>
                        <div style={{ display: 'flex', gap: 12 }}>
                            <AlertCircle size={20} color="#856404" style={{ flexShrink: 0, marginTop: 2 }} />
                            <div>
                                <div style={{ fontWeight: 600, color: '#856404', marginBottom: 4 }}>
                                    Why do we need your location?
                                </div>
                                <div style={{ fontSize: '0.85rem', color: '#856404', lineHeight: 1.6 }}>
                                    FleetFlow needs access to your device's location to:
                                    <ul style={{ margin: '8px 0 0 0', paddingLeft: 20 }}>
                                        <li>Track your position during trips</li>
                                        <li>Show your location on the live map</li>
                                        <li>Help dispatchers assign trips efficiently</li>
                                        <li>Provide accurate trip progress updates</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Browser-specific instructions */}
                    <div>
                        <h4 style={{ marginBottom: 12, fontSize: '0.95rem' }}>
                            How to enable location in {instructions.browser}:
                        </h4>
                        <ol style={{
                            margin: 0,
                            paddingLeft: 20,
                            fontSize: '0.9rem',
                            lineHeight: 1.8,
                            color: 'var(--text-600)'
                        }}>
                            {instructions.steps.map((step, index) => (
                                <li key={index} style={{ marginBottom: 8 }}>
                                    {step}
                                </li>
                            ))}
                        </ol>
                    </div>

                    {/* Additional help */}
                    <div style={{
                        marginTop: 20,
                        padding: 12,
                        background: '#f8f9fa',
                        borderRadius: 8,
                        fontSize: '0.85rem',
                        color: 'var(--text-500)'
                    }}>
                        <strong>Still having trouble?</strong> Contact your fleet manager or IT support for assistance.
                    </div>
                </div>

                {/* Footer */}
                <div style={{
                    padding: '16px 24px',
                    borderTop: '1px solid var(--border)',
                    display: 'flex',
                    justifyContent: 'flex-end'
                }}>
                    <button
                        onClick={onClose}
                        className="btn btn-primary"
                    >
                        Got it
                    </button>
                </div>
            </div>
        </div>
    );
}
