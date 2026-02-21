-- Migration for Driver Location & RBAC Feature
-- Requirements: 13.2, 13.3, 7.5
-- This migration adds location tracking enhancements and audit logging

-- ============================================================================
-- 1. ALTER driver_locations table - Add new columns for location tracking
-- ============================================================================

ALTER TABLE driver_locations 
ADD COLUMN IF NOT EXISTS sharing_active BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS accuracy DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_error VARCHAR(255);

-- Add check constraint for accuracy (must be non-negative)
ALTER TABLE driver_locations 
ADD CONSTRAINT chk_driver_locations_accuracy 
CHECK (accuracy IS NULL OR accuracy >= 0);

-- Create index for active location queries (partial index for performance)
CREATE INDEX IF NOT EXISTS idx_driver_locations_sharing_active 
ON driver_locations(sharing_active) 
WHERE sharing_active = TRUE;

-- ============================================================================
-- 2. CREATE location_history table - Store historical location data
-- ============================================================================

CREATE TABLE IF NOT EXISTS location_history (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_location_history_driver 
        FOREIGN KEY (driver_id) 
        REFERENCES drivers(id) 
        ON DELETE CASCADE,
    
    -- Check constraints for valid coordinate ranges
    CONSTRAINT chk_location_history_latitude 
        CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT chk_location_history_longitude 
        CHECK (longitude >= -180 AND longitude <= 180),
    CONSTRAINT chk_location_history_accuracy 
        CHECK (accuracy IS NULL OR accuracy >= 0),
    CONSTRAINT chk_location_history_speed 
        CHECK (speed IS NULL OR speed >= 0),
    CONSTRAINT chk_location_history_heading 
        CHECK (heading IS NULL OR (heading >= 0 AND heading <= 360))
);

-- Index for history queries by driver and time (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_location_history_driver_time 
ON location_history(driver_id, recorded_at DESC);

-- Index for purge operations (to efficiently delete old records)
CREATE INDEX IF NOT EXISTS idx_location_history_recorded_at 
ON location_history(recorded_at);

-- ============================================================================
-- 3. CREATE audit_logs table - Track unauthorized access attempts
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    resource_uri VARCHAR(500),
    http_method VARCHAR(10),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint (nullable to preserve logs if user is deleted)
    CONSTRAINT fk_audit_logs_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL
);

-- Index for audit queries by user and time
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_time 
ON audit_logs(user_id, created_at DESC);

-- Index for audit queries by action type and time
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_time 
ON audit_logs(action_type, created_at DESC);

-- Index for audit queries by username (for when user_id is null)
CREATE INDEX IF NOT EXISTS idx_audit_logs_username 
ON audit_logs(username);

-- ============================================================================
-- Migration Complete
-- ============================================================================

-- Add comments for documentation
COMMENT ON TABLE location_history IS 'Stores historical location data for drivers with 90-day retention policy';
COMMENT ON TABLE audit_logs IS 'Tracks unauthorized access attempts and security events';
COMMENT ON COLUMN driver_locations.sharing_active IS 'Indicates if driver is actively sharing location';
COMMENT ON COLUMN driver_locations.accuracy IS 'Location accuracy in meters from Geolocation API';
COMMENT ON COLUMN driver_locations.consecutive_failures IS 'Count of consecutive location update failures';
COMMENT ON COLUMN driver_locations.last_error IS 'Last error message from location update attempt';
