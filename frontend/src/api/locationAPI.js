import axios from 'axios';
import { toast } from 'react-hot-toast';

const API_BASE = 'http://localhost:8080/api';

/**
 * Location API Client
 * 
 * Provides API methods for driver location sharing with comprehensive error handling
 * and retry logic for network failures.
 * 
 * Requirements: 2.2, 2.4, 3.1, 3.4, 11.4, 15.3
 */

// Create axios instance with base configuration
const locationApi = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor to add auth token
locationApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleetflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
locationApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized - logout user
    if (error.response?.status === 401) {
      toast.error('Session expired. Please log in again.');
      localStorage.removeItem('fleetflow_token');
      localStorage.removeItem('fleetflow_user');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // Handle 403 Forbidden - show error
    if (error.response?.status === 403) {
      toast.error('You do not have permission to perform this action');
      return Promise.reject(error);
    }

    // Handle 429 Too Many Requests - show rate limit message
    if (error.response?.status === 429) {
      toast.error('Too many requests. Please wait before trying again.');
      return Promise.reject(error);
    }

    // Handle 500 Server Error - retry with exponential backoff
    if (error.response?.status === 500) {
      // Check if we've already retried
      const retryCount = originalRequest._retryCount || 0;
      const maxRetries = 3;

      if (retryCount < maxRetries) {
        originalRequest._retryCount = retryCount + 1;

        // Calculate exponential backoff delay
        const delay = Math.pow(2, retryCount) * 1000; // 1s, 2s, 4s

        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, delay));

        // Retry the request
        return locationApi(originalRequest);
      } else {
        toast.error('Server error. Please try again later.');
        return Promise.reject(error);
      }
    }

    // Handle network errors (no response from server)
    if (!error.response) {
      // Check if we've already retried
      const retryCount = originalRequest._retryCount || 0;
      const maxRetries = 3;

      if (retryCount < maxRetries) {
        originalRequest._retryCount = retryCount + 1;

        // Calculate exponential backoff delay
        const delay = Math.pow(2, retryCount) * 1000;

        toast.error(`Connection error. Retrying... (${retryCount + 1}/${maxRetries})`);

        // Wait before retrying
        await new Promise(resolve => setTimeout(resolve, delay));

        // Retry the request
        return locationApi(originalRequest);
      } else {
        toast.error('Network error. Please check your connection.');
        return Promise.reject(error);
      }
    }

    // Handle other errors
    return Promise.reject(error);
  }
);

/**
 * Location API Service
 */
export const locationAPI = {
  /**
   * Start location sharing for the current driver
   * @param {Object} locationData - Location data (latitude, longitude, accuracy, speed, heading)
   * @returns {Promise} API response
   */
  startLocationSharing: (locationData) => {
    return locationApi.post('/drivers/locations/start', locationData);
  },

  /**
   * Stop location sharing for the current driver
   * @returns {Promise} API response
   */
  stopLocationSharing: () => {
    return locationApi.post('/drivers/locations/stop');
  },

  /**
   * Update current location for the driver
   * @param {Object} locationData - Location data (latitude, longitude, accuracy, speed, heading)
   * @returns {Promise} API response
   */
  updateLocation: (locationData) => {
    return locationApi.put('/drivers/locations', locationData);
  },

  /**
   * Get location for a specific driver
   * @param {number} driverId - Driver ID
   * @returns {Promise} API response with driver location
   */
  getDriverLocation: (driverId) => {
    return locationApi.get(`/drivers/locations/${driverId}`);
  },

  /**
   * Get location history for a driver
   * @param {number} driverId - Driver ID
   * @param {string} startDate - Start date (ISO format: YYYY-MM-DD)
   * @param {string} endDate - End date (ISO format: YYYY-MM-DD)
   * @returns {Promise} API response with location history
   */
  getLocationHistory: (driverId, startDate, endDate) => {
    return locationApi.get(`/drivers/locations/${driverId}/history`, {
      params: { startDate, endDate }
    });
  },

  /**
   * Get all active driver locations
   * @returns {Promise} API response with all active locations
   */
  getAllLocations: () => {
    return locationApi.get('/drivers/locations');
  }
};

export default locationAPI;
