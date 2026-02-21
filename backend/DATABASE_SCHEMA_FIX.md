# Database Schema Fix Guide

## Problem
The application is failing with error: `column dl1_0.sharing_active does not exist`

This happens because the database schema is out of sync with the JPA entity definitions.

## Root Cause
- The project was using `spring.jpa.hibernate.ddl-auto=update` without Flyway
- The `DriverLocation` entity has a `sharingActive` field, but the database table doesn't have the corresponding column
- Migration files exist but were never applied because Flyway wasn't configured

## Solution

### Option 1: Quick Fix (Manual SQL - Recommended for immediate fix)

Run the SQL script to add the missing column:

```bash
# Connect to your PostgreSQL database and run:
psql -h ep-wandering-wildflower-a1exphgy-pooler.ap-southeast-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb \
     -f backend/fix_driver_locations_schema.sql
```

Or execute directly in your database client:

```sql
ALTER TABLE driver_locations 
ADD COLUMN IF NOT EXISTS sharing_active BOOLEAN DEFAULT FALSE NOT NULL;
```

### Option 2: Enable Flyway and Restart (Proper long-term solution)

1. **Install Flyway dependencies** (already added to pom.xml):
   ```bash
   cd backend
   mvn clean install
   ```

2. **Flyway is now configured** in `application.properties`:
   - `spring.flyway.enabled=true`
   - `spring.flyway.baseline-on-migrate=true`
   - `spring.jpa.hibernate.ddl-auto=validate` (changed from `update`)

3. **Restart the application**:
   ```bash
   mvn spring-boot:run
   ```

   Flyway will:
   - Baseline the existing schema
   - Apply the V001 migration (which includes the sharing_active column)
   - Keep schema in sync going forward

### Option 3: Full Reset (Only if you can lose data)

```sql
-- Drop and recreate the table (WARNING: DATA LOSS)
DROP TABLE IF EXISTS driver_locations CASCADE;
```

Then restart the application - Hibernate will recreate the table with all columns.

## Verification

After applying the fix, verify the column exists:

```sql
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'driver_locations'
ORDER BY ordinal_position;
```

You should see `sharing_active` in the list.

## What Changed

1. **pom.xml**: Added Flyway dependencies
   - `flyway-core`
   - `flyway-database-postgresql`

2. **application.properties**: 
   - Changed `spring.jpa.hibernate.ddl-auto` from `update` to `validate`
   - Enabled Flyway with baseline-on-migrate

3. **Created**: `fix_driver_locations_schema.sql` for manual fix

## Going Forward

- All schema changes should be done via Flyway migrations in `src/main/resources/db/migration/`
- Follow naming convention: `V{version}__{description}.sql`
- Hibernate will validate schema matches entities but won't modify it
- This prevents schema drift and makes deployments safer
