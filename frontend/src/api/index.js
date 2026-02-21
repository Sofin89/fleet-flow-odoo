import axios from 'axios';

const API_BASE = '/api';

const api = axios.create({
    baseURL: API_BASE,
    headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('fleetflow_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('fleetflow_token');
            localStorage.removeItem('fleetflow_user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// Auth
export const authAPI = {
    login: (data) => api.post('/auth/login', data),
    register: (data) => api.post('/auth/register', data),
};

// Dashboard
export const dashboardAPI = {
    getKPIs: () => api.get('/dashboard/kpis'),
};

// Vehicles
export const vehicleAPI = {
    getAll: (params) => api.get('/vehicles', { params }),
    getById: (id) => api.get(`/vehicles/${id}`),
    create: (data) => api.post('/vehicles', data),
    update: (id, data) => api.put(`/vehicles/${id}`, data),
    updateStatus: (id, status) => api.patch(`/vehicles/${id}/status`, { status }),
    getAvailable: () => api.get('/vehicles/available'),
};

// Drivers
export const driverAPI = {
    getAll: (params) => api.get('/drivers', { params }),
    getById: (id) => api.get(`/drivers/${id}`),
    create: (data) => api.post('/drivers', data),
    update: (id, data) => api.put(`/drivers/${id}`, data),
    updateStatus: (id, status) => api.patch(`/drivers/${id}/status`, { status }),
    getAvailable: (category) => api.get('/drivers/available', { params: { category } }),
    getLocations: () => api.get('/drivers/locations'),
};

// Trips
export const tripAPI = {
    getAll: (params) => api.get('/trips', { params }),
    getById: (id) => api.get(`/trips/${id}`),
    create: (data) => api.post('/trips', data),
    dispatch: (id) => api.patch(`/trips/${id}/dispatch`),
    complete: (id, data) => api.patch(`/trips/${id}/complete`, data || {}),
    cancel: (id) => api.patch(`/trips/${id}/cancel`),
};

// Maintenance
export const maintenanceAPI = {
    getAll: (params) => api.get('/maintenance', { params }),
    getByVehicle: (vehicleId, params) => api.get(`/maintenance/vehicle/${vehicleId}`, { params }),
    create: (data) => api.post('/maintenance', data),
    complete: (id) => api.patch(`/maintenance/${id}/complete`),
};

// Fuel Logs
export const fuelLogAPI = {
    getAll: (params) => api.get('/fuel-logs', { params }),
    getByVehicle: (vehicleId, params) => api.get(`/fuel-logs/vehicle/${vehicleId}`, { params }),
    create: (data) => api.post('/fuel-logs', data),
};

// Reports
export const reportAPI = {
    fuelEfficiency: () => api.get('/reports/fuel-efficiency'),
    vehicleROI: () => api.get('/reports/vehicle-roi'),
    operationalCosts: () => api.get('/reports/operational-costs'),
};

// Map Tracking
export const mapAPI = {
    getMarkers: () => api.get('/map/markers'),
};

export default api;
