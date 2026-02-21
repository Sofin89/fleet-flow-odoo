-- Verification script for V001 migration
-- Run this after applying the migration to verify all changes were applied correctly

\echo '========================================='
\echo 'Verifying Driver Location & RBAC Migration'
\echo '========================================='
\echo ''

-- ============================================================================
-- 1. Verify driver_locations table columns
-- ============================================================================

\echo '1. Checking driver_locations table columns...'
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'driver_locations'
    AND column_name IN ('sharing_active', 'accuracy', 'consecutive_failures', 'last_error')
ORDER BY column_name;

\echo ''
\echo '   Expected: 4 columns (sharing_active, accuracy, consecutive_failures, last_error)'
\echo ''

-- ============================================================================
-- 2. Verify driver_locations constraints
-- ============================================================================

\echo '2. Checking driver_locations constraints...'
SELECT 
    constraint_name,
    constraint_type
FROM information_schema.table_constraints 
WHERE table_name = 'driver_locations'
    AND constraint_name = 'chk_driver_locations_accuracy';

\echo ''
\echo '   Expected: 1 constraint (chk_driver_locations_accuracy)'
\echo ''

-- ============================================================================
-- 3. Verify driver_locations indexes
-- ============================================================================

\echo '3. Checking driver_locations indexes...'
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'driver_locations'
    AND indexname = 'idx_driver_locations_sharing_active';

\echo ''
\echo '   Expected: 1 index (idx_driver_locations_sharing_active)'
\echo ''

-- ============================================================================
-- 4. Verify location_history table exists
-- ============================================================================

\echo '4. Checking location_history table...'
SELECT 
    table_name,
    table_type
FROM information_schema.tables 
WHERE table_name = 'location_history';

\echo ''
\echo '   Expected: 1 table (location_history)'
\echo ''

-- ============================================================================
-- 5. Verify location_history columns
-- ============================================================================

\echo '5. Checking location_history columns...'
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'location_history'
ORDER BY ordinal_position;

\echo ''
\echo '   Expected: 8 columns (id, driver_id, latitude, longitude, accuracy, speed, heading, recorded_at)'
\echo ''

-- ============================================================================
-- 6. Verify location_history constraints
-- ============================================================================

\echo '6. Checking location_history constraints...'
SELECT 
    constraint_name,
    constraint_type
FROM information_schema.table_constraints 
WHERE table_name = 'location_history'
ORDER BY constraint_name;

\echo ''
\echo '   Expected: 6 constraints (primary key, foreign key, 5 check constraints)'
\echo ''

-- ============================================================================
-- 7. Verify location_history indexes
-- ============================================================================

\echo '7. Checking location_history indexes...'
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'location_history'
ORDER BY indexname;

\echo ''
\echo '   Expected: 3 indexes (primary key + 2 custom indexes)'
\echo ''

-- ============================================================================
-- 8. Verify audit_logs table exists
-- ============================================================================

\echo '8. Checking audit_logs table...'
SELECT 
    table_name,
    table_type
FROM information_schema.tables 
WHERE table_name = 'audit_logs';

\echo ''
\echo '   Expected: 1 table (audit_logs)'
\echo ''

-- ============================================================================
-- 9. Verify audit_logs columns
-- ============================================================================

\echo '9. Checking audit_logs columns...'
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'audit_logs'
ORDER BY ordinal_position;

\echo ''
\echo '   Expected: 10 columns (id, user_id, username, user_role, action_type, resource_uri, http_method, ip_address, user_agent, created_at)'
\echo ''

-- ============================================================================
-- 10. Verify audit_logs indexes
-- ============================================================================

\echo '10. Checking audit_logs indexes...'
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'audit_logs'
ORDER BY indexname;

\echo ''
\echo '   Expected: 4 indexes (primary key + 3 custom indexes)'
\echo ''

-- ============================================================================
-- 11. Summary
-- ============================================================================

\echo ''
\echo '========================================='
\echo 'Verification Summary'
\echo '========================================='

SELECT 
    'driver_locations columns' AS check_item,
    COUNT(*) AS actual_count,
    4 AS expected_count,
    CASE WHEN COUNT(*) = 4 THEN '✓ PASS' ELSE '✗ FAIL' END AS status
FROM information_schema.columns 
WHERE table_name = 'driver_locations'
    AND column_name IN ('sharing_active', 'accuracy', 'consecutive_failures', 'last_error')

UNION ALL

SELECT 
    'location_history table',
    COUNT(*),
    1,
    CASE WHEN COUNT(*) = 1 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM information_schema.tables 
WHERE table_name = 'location_history'

UNION ALL

SELECT 
    'location_history columns',
    COUNT(*),
    8,
    CASE WHEN COUNT(*) = 8 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM information_schema.columns 
WHERE table_name = 'location_history'

UNION ALL

SELECT 
    'audit_logs table',
    COUNT(*),
    1,
    CASE WHEN COUNT(*) = 1 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM information_schema.tables 
WHERE table_name = 'audit_logs'

UNION ALL

SELECT 
    'audit_logs columns',
    COUNT(*),
    10,
    CASE WHEN COUNT(*) = 10 THEN '✓ PASS' ELSE '✗ FAIL' END
FROM information_schema.columns 
WHERE table_name = 'audit_logs';

\echo ''
\echo 'Verification complete!'
\echo ''
