# Migration Guide: Driver Location & RBAC

## Overview

This guide explains the database schema changes for the Driver Location & RBAC feature and how to apply them.

## What's Changed

### 1. Enhanced driver_locations Table

**New Columns:**
- `sharing_active` (BOOLEAN, default: FALSE) - Indicates if the driver is actively sharing their location
- `accuracy` (DOUBLE PRECISION, nullable) - Location accuracy in meters from the Geolocation API
- `consecutive_failures` (INTEGER, default: 0) - Tracks consecutive location update failures for auto-stop functionality
- `last_error` (VARCHAR(255), nullable) - Stores the last error message for debugging

**New Constraints:**
- Check constraint ensuring accuracy is non-negative (when not null)

**New Indexes:**
- Partial index on `sharing_active` for efficient queries of active drivers

### 2. New location_history Table

Stores historical location data for drivers with the following structure:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGSERIAL | NO | Primary key |
| driver_id | BIGINT | NO | Foreign key to drivers table |
| latitude | DOUBLE PRECISION | NO | Latitude coordinate (-90 to 90) |
| longitude | DOUBLE PRECISION | NO | Longitude coordinate (-180 to 180) |
| accuracy | DOUBLE PRECISION | YES | Location accuracy in meters |
| speed | DOUBLE PRECISION | YES | Speed in meters per second |
| heading | DOUBLE PRECISION | YES | Heading in degrees (0-360) |
| recorded_at | TIMESTAMP | NO | When the location was recorded |

**Constraints:**
- Foreign key to drivers table with CASCADE delete
- Check constraints for valid coordinate ranges
- Check constraints for non-negative accuracy and speed
- Check constraint for heading range (0-360)

**Indexes:**
- Composite index on (driver_id, recorded_at DESC) for efficient history queries
- Index on recorded_at for efficient purge operations

**Data Retention:**
- Records older than 90 days are automatically purged by a scheduled job
- Queries are limited to 30-day ranges

### 3. New audit_logs Table

Tracks security events and unauthorized access attempts:

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGSERIAL | NO | Primary key |
| user_id | BIGINT | YES | Foreign key to users table |
| username | VARCHAR(255) | NO | Username (preserved if user deleted) |
| user_role | VARCHAR(50) | NO | User's role at time of action |
| action_type | VARCHAR(50) | NO | Type of action (e.g., UNAUTHORIZED_ACCESS) |
| resource_uri | VARCHAR(500) | YES | Requested resource URI |
| http_method | VARCHAR(10) | YES | HTTP method (GET, POST, etc.) |
| ip_address | VARCHAR(45) | YES | Client IP address |
| user_agent | TEXT | YES | Client user agent string |
| created_at | TIMESTAMP | NO | When the event occurred |

**Constraints:**
- Foreign key to users table with SET NULL on delete (preserves audit trail)

**Indexes:**
- Composite index on (user_id, created_at DESC) for user-specific queries
- Composite index on (action_type, created_at DESC) for action-type queries
- Index on username for queries when user_id is null

## Quick Start

### For Development (Current Setup)

The application uses Hibernate's auto-update mode, so the schema will be updated automatically when you:

1. Update the entity classes (DriverLocation, LocationHistory, AuditLog)
2. Restart the application

However, you can also apply the migration manually for better control:

```bash
# Connect to your database
psql -h ep-wandering-wildflower-a1exphgy-pooler.ap-southeast-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb

# Apply the migration
\i backend/src/main/resources/db/migration/V001__driver_location_and_rbac_schema.sql

# Verify the migration
\i backend/src/main/resources/db/migration/verify_migration.sql
```

### For Production (Recommended)

Use Flyway for automated, version-controlled migrations:

1. Add Flyway to your `pom.xml` (see README.md)
2. Change `spring.jpa.hibernate.ddl-auto=validate` in application.properties
3. Restart the application - migrations apply automatically

## Verification Steps

After applying the migration, verify the changes:

```sql
-- 1. Check new columns in driver_locations
\d driver_locations

-- 2. Check location_history table
\d location_history

-- 3. Check audit_logs table
\d audit_logs

-- 4. Verify indexes
\di idx_driver_locations_sharing_active
\di idx_location_history_driver_time
\di idx_location_history_recorded_at
\di idx_audit_logs_user_time
\di idx_audit_logs_action_time
\di idx_audit_logs_username

-- 5. Run the verification script
\i backend/src/main/resources/db/migration/verify_migration.sql
```

## Rollback

If you need to rollback the migration:

```bash
psql -h <host> -U <username> -d <database>
\i backend/src/main/resources/db/migration/V001__driver_location_and_rbac_schema_rollback.sql
```

**WARNING:** Rollback will delete all data in location_history and audit_logs tables!

## Impact on Existing Data

- **driver_locations**: Existing records will have new columns set to default values:
  - `sharing_active` = FALSE
  - `accuracy` = NULL
  - `consecutive_failures` = 0
  - `last_error` = NULL

- **No data loss**: All existing location data is preserved

## Performance Considerations

### Indexes
- Partial index on `sharing_active` improves queries for active drivers
- Composite indexes optimize common query patterns
- Indexes are created with `IF NOT EXISTS` to avoid errors on re-run

### Data Volume
- `location_history` will grow over time (one record per location update)
- With 30-second intervals and 100 drivers, expect ~288,000 records/day
- Automatic purge job keeps data under 90 days (~26M records max)
- Indexes ensure queries remain fast even with large datasets

### Query Optimization
```sql
-- Efficient: Uses idx_location_history_driver_time
SELECT * FROM location_history 
WHERE driver_id = 123 
ORDER BY recorded_at DESC 
LIMIT 100;

-- Efficient: Uses idx_driver_locations_sharing_active
SELECT * FROM driver_locations 
WHERE sharing_active = TRUE;

-- Efficient: Uses idx_audit_logs_action_time
SELECT * FROM audit_logs 
WHERE action_type = 'UNAUTHORIZED_ACCESS' 
ORDER BY created_at DESC 
LIMIT 50;
```

## Troubleshooting

### Migration fails with "relation already exists"
The migration uses `IF NOT EXISTS` clauses, so this shouldn't happen. If it does, check if a partial migration was applied.

### Migration fails with "column already exists"
Same as above - the migration is idempotent. Check for partial application.

### Foreign key constraint violation
Ensure the `drivers` and `users` tables exist before running the migration.

### Performance issues after migration
Run `ANALYZE` on the new tables to update statistics:
```sql
ANALYZE driver_locations;
ANALYZE location_history;
ANALYZE audit_logs;
```

## Next Steps

After applying the migration:

1. Update entity classes (Task 1.2, 1.3, 1.4)
2. Create repository interfaces
3. Implement service layer
4. Add API endpoints
5. Test the new functionality

## Questions?

If you encounter issues or have questions about the migration, refer to:
- Design document: `.kiro/specs/driver-location-and-rbac/design.md`
- Requirements: `.kiro/specs/driver-location-and-rbac/requirements.md`
- Tasks: `.kiro/specs/driver-location-and-rbac/tasks.md`
