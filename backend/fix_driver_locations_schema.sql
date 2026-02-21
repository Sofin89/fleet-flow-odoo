-- Fix for missing sharing_active column in driver_locations table
-- This script adds the missing column that was added to the entity but not reflected in the database

-- Add the missing column
ALTER TABLE driver_locations 
ADD COLUMN IF NOT EXISTS sharing_active BOOLEAN DEFAULT FALSE NOT NULL;

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'driver_locations'
ORDER BY ordinal_position;
