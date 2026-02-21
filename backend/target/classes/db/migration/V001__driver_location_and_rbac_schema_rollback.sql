-- Rollback script for Driver Location & RBAC Feature Migration
-- This script reverts the changes made in V001__driver_location_and_rbac_schema.sql
-- WARNING: This will delete all location history and audit log data

-- ============================================================================
-- 1. DROP audit_logs table
-- ============================================================================

DROP INDEX IF EXISTS idx_audit_logs_username;
DROP INDEX IF EXISTS idx_audit_logs_action_time;
DROP INDEX IF EXISTS idx_audit_logs_user_time;
DROP TABLE IF EXISTS audit_logs;

-- ============================================================================
-- 2. DROP location_history table
-- ============================================================================

DROP INDEX IF EXISTS idx_location_history_recorded_at;
DROP INDEX IF EXISTS idx_location_history_driver_time;
DROP TABLE IF EXISTS location_history;

-- ============================================================================
-- 3. REVERT driver_locations table changes
-- ============================================================================

DROP INDEX IF EXISTS idx_driver_locations_sharing_active;

ALTER TABLE driver_locations 
DROP CONSTRAINT IF EXISTS chk_driver_locations_accuracy;

ALTER TABLE driver_locations 
DROP COLUMN IF EXISTS last_error,
DROP COLUMN IF EXISTS consecutive_failures,
DROP COLUMN IF EXISTS accuracy,
DROP COLUMN IF EXISTS sharing_active;

-- ============================================================================
-- Rollback Complete
-- ============================================================================
