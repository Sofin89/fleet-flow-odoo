/**
 * GeolocationService
 * 
 * Encapsulates browser Geolocation API interactions and manages location tracking lifecycle.
 * Provides high-accuracy location tracking with error handling, retry logic, and periodic updates.
 * 
 * Requirements: 1.1, 1.3, 1.5, 2.2, 2.4, 3.5, 12.1, 12.2, 12.5, 15.3, 15.4
 */

// High accuracy configuration for geolocation requests
const HIGH_ACCURACY_OPTIONS = {
  enableHighAccuracy: true,
  timeout: 10000,
  maximumAge: 0
};

// Tracking interval: 30 seconds
const TRACKING_INTERVAL = 30000;

// Maximum consecutive failures before auto-stop
const MAX_CONSECUTIVE_FAILURES = 5;

// Error codes from Geolocation API
const GEOLOCATION_ERROR_CODES = {
  PERMISSION_DENIED: 1,
  POSITION_UNAVAILABLE: 2,
  TIMEOUT: 3
};

class GeolocationService {
  constructor() {
    this.watchId = null;
    this.intervalId = null;
    this.isTracking = false;
    this.consecutiveFailures = 0;
    this.retryCount = 0;
    this.onSuccessCallback = null;
    this.onErrorCallback = null;
  }

  /**
   * Check if geolocation is supported by the browser
   * @returns {boolean} True if geolocation is supported
   */
  isSupported() {
    return 'geolocation' in navigator;
  }

  /**
   * Check geolocation permission status
   * @returns {Promise<string>} Permission state: 'granted', 'denied', or 'prompt'
   */
  async checkPermission() {
    if (!this.isSupported()) {
      return 'denied';
    }

    try {
      // Check if Permissions API is available
      if (navigator.permissions && navigator.permissions.query) {
        const result = await navigator.permissions.query({ name: 'geolocation' });
        return result.state;
      }
      
      // Fallback: permissions API not available
      return 'prompt';
    } catch (error) {
      console.error('Error checking geolocation permission:', error);
      return 'prompt';
    }
  }

  /**
   * Get current position once
   * @param {Function} onSuccess - Callback for successful position retrieval
   * @param {Function} onError - Callback for errors
   * @param {Object} options - Geolocation options (defaults to HIGH_ACCURACY_OPTIONS)
   */
  getCurrentPosition(onSuccess, onError, options = HIGH_ACCURACY_OPTIONS) {
    if (!this.isSupported()) {
      const error = new Error('Geolocation is not supported by this browser');
      error.code = 0;
      if (onError) onError(error);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        if (onSuccess) onSuccess(position);
      },
      (error) => {
        this._handleError(error, onError);
      },
      options
    );
  }

  /**
   * Start continuous location tracking with 30-second interval
   * @param {Function} onSuccess - Callback for successful position updates
   * @param {Function} onError - Callback for errors
   * @param {Object} options - Geolocation options (defaults to HIGH_ACCURACY_OPTIONS)
   */
  startTracking(onSuccess, onError, options = HIGH_ACCURACY_OPTIONS) {
    if (!this.isSupported()) {
      const error = new Error('Geolocation is not supported by this browser');
      error.code = 0;
      if (onError) onError(error);
      return;
    }

    if (this.isTracking) {
      console.warn('Location tracking is already active');
      return;
    }

    this.isTracking = true;
    this.consecutiveFailures = 0;
    this.retryCount = 0;
    this.onSuccessCallback = onSuccess;
    this.onErrorCallback = onError;

    // Get initial position immediately
    this._requestPosition(options);

    // Set up periodic updates every 30 seconds
    this.intervalId = setInterval(() => {
      this._requestPosition(options);
    }, TRACKING_INTERVAL);
  }

  /**
   * Stop location tracking
   */
  stopTracking() {
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }

    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }

    this.isTracking = false;
    this.consecutiveFailures = 0;
    this.retryCount = 0;
    this.onSuccessCallback = null;
    this.onErrorCallback = null;
  }

  /**
   * Request current position (internal method)
   * @private
   */
  _requestPosition(options) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.consecutiveFailures = 0;
        this.retryCount = 0;
        
        if (this.onSuccessCallback) {
          this.onSuccessCallback(position);
        }
      },
      (error) => {
        this.consecutiveFailures++;
        
        // Check if we've hit the max consecutive failures
        if (this.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
          this._autoStopTracking();
          return;
        }

        this._handleError(error, this.onErrorCallback, options);
      },
      options
    );
  }

  /**
   * Handle geolocation errors with retry logic
   * @private
   */
  _handleError(error, onError, options = HIGH_ACCURACY_OPTIONS) {
    console.error('Geolocation error:', error.code, error.message);

    switch (error.code) {
      case GEOLOCATION_ERROR_CODES.PERMISSION_DENIED:
        // Permission denied - no retry
        if (onError) {
          onError({
            code: error.code,
            message: 'Location access denied. Please enable location permissions in your browser settings.',
            type: 'PERMISSION_DENIED',
            retry: false
          });
        }
        break;

      case GEOLOCATION_ERROR_CODES.POSITION_UNAVAILABLE:
        // Position unavailable - retry with exponential backoff
        if (this.retryCount < 3) {
          const delay = Math.pow(2, this.retryCount) * 1000;
          this.retryCount++;
          
          setTimeout(() => {
            if (this.isTracking) {
              this._requestPosition(options);
            }
          }, delay);

          if (onError) {
            onError({
              code: error.code,
              message: 'Unable to determine your location. Retrying...',
              type: 'POSITION_UNAVAILABLE',
              retry: true,
              retryCount: this.retryCount,
              retryDelay: delay
            });
          }
        } else {
          if (onError) {
            onError({
              code: error.code,
              message: 'Unable to determine your location after multiple attempts.',
              type: 'POSITION_UNAVAILABLE',
              retry: false
            });
          }
        }
        break;

      case GEOLOCATION_ERROR_CODES.TIMEOUT:
        // Timeout - retry with increased timeout
        if (this.retryCount < 3) {
          const newTimeout = options.timeout + (this.retryCount * 5000);
          this.retryCount++;
          
          const newOptions = {
            ...options,
            timeout: newTimeout
          };

          setTimeout(() => {
            if (this.isTracking) {
              this._requestPosition(newOptions);
            }
          }, 1000);

          if (onError) {
            onError({
              code: error.code,
              message: 'Location request timed out. Retrying with extended timeout...',
              type: 'TIMEOUT',
              retry: true,
              retryCount: this.retryCount,
              newTimeout: newTimeout
            });
          }
        } else {
          if (onError) {
            onError({
              code: error.code,
              message: 'Location request timed out after multiple attempts.',
              type: 'TIMEOUT',
              retry: false
            });
          }
        }
        break;

      default:
        // Unknown error
        if (onError) {
          onError({
            code: error.code,
            message: 'An unknown error occurred while accessing your location.',
            type: 'UNKNOWN',
            retry: false
          });
        }
        break;
    }
  }

  /**
   * Auto-stop tracking after consecutive failures
   * @private
   */
  _autoStopTracking() {
    console.warn(`Auto-stopping location tracking after ${MAX_CONSECUTIVE_FAILURES} consecutive failures`);
    
    const onError = this.onErrorCallback;
    this.stopTracking();

    if (onError) {
      onError({
        code: 'AUTO_STOP',
        message: 'Location sharing stopped due to repeated failures. Please check your connection and try again.',
        type: 'AUTO_STOP',
        retry: false,
        consecutiveFailures: MAX_CONSECUTIVE_FAILURES
      });
    }
  }
}

// Export singleton instance
const geolocationService = new GeolocationService();

export default geolocationService;
export { HIGH_ACCURACY_OPTIONS, TRACKING_INTERVAL, MAX_CONSECUTIVE_FAILURES, GEOLOCATION_ERROR_CODES };
