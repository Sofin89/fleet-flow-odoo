# Database Migrations

This directory contains SQL migration scripts for the FleetFlow application.

## Current Setup

The application currently uses Hibernate's `ddl-auto=update` mode, which automatically updates the database schema based on entity changes. However, for production environments and better control over schema changes, we provide explicit SQL migration scripts.

## Migration Files

### V001__driver_location_and_rbac_schema.sql
**Feature:** Driver Location & RBAC  
**Requirements:** 13.2, 13.3, 7.5

This migration adds:
1. New columns to `driver_locations` table:
   - `sharing_active` (BOOLEAN) - Tracks if driver is actively sharing location
   - `accuracy` (DOUBLE PRECISION) - Location accuracy in meters
   - `consecutive_failures` (INTEGER) - Count of consecutive update failures
   - `last_error` (VARCHAR) - Last error message from location updates

2. New `location_history` table:
   - Stores historical location data for drivers
   - Includes indexes for efficient querying
   - Enforces 90-day retention policy (via scheduled job)
   - Cascades on driver deletion

3. New `audit_logs` table:
   - Tracks unauthorized access attempts
   - Records user actions and security events
   - Preserves logs even if user is deleted

### V001__driver_location_and_rbac_schema_rollback.sql
Rollback script to revert the V001 migration changes.

## How to Apply Migrations

### Option 1: Manual Application (Current Setup)

Since the project uses `spring.jpa.hibernate.ddl-auto=update`, the schema will be automatically updated when you update the entity classes. However, you can also apply migrations manually:

```bash
# Connect to your PostgreSQL database
psql -h <host> -U <username> -d <database>

# Apply the migration
\i backend/src/main/resources/db/migration/V001__driver_location_and_rbac_schema.sql

# To rollback (if needed)
\i backend/src/main/resources/db/migration/V001__driver_location_and_rbac_schema_rollback.sql
```

### Option 2: Using Flyway (Recommended for Production)

To enable Flyway for automated migrations:

1. Add Flyway dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

2. Update `application.properties`:
```properties
# Disable Hibernate auto-update
spring.jpa.hibernate.ddl-auto=validate

# Enable Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

3. Restart the application - Flyway will automatically apply pending migrations.

### Option 3: Using Liquibase

Alternatively, you can use Liquibase:

1. Add Liquibase dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

2. Convert SQL scripts to Liquibase changelog format or use SQL directly.

## Migration Naming Convention

Migrations follow the pattern: `V{version}__{description}.sql`

- `V` prefix indicates a versioned migration
- Version number (e.g., `001`) ensures proper ordering
- Double underscore `__` separates version from description
- Description uses snake_case

## Verification

After applying migrations, verify the changes:

```sql
-- Check driver_locations columns
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'driver_locations';

-- Check location_history table exists
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'location_history';

-- Check audit_logs table exists
SELECT table_name 
FROM information_schema.tables 
WHERE table_name = 'audit_logs';

-- Check indexes
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename IN ('driver_locations', 'location_history', 'audit_logs');
```

## Notes

- All migrations are idempotent (safe to run multiple times) using `IF NOT EXISTS` clauses
- Foreign key constraints ensure referential integrity
- Check constraints validate data ranges (e.g., latitude between -90 and 90)
- Indexes are optimized for common query patterns
- Comments are added to tables and columns for documentation
