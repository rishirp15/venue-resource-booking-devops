-- ===================================================================
-- Venue Resource Booking System - PostgreSQL Verification Queries
-- Run this script to verify data integrity and row counts post-migration.
-- ===================================================================

-- 1. ROW COUNTS VERIFICATION
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'venues' AS table_name, COUNT(*) AS row_count FROM venues
UNION ALL
SELECT 'bookings' AS table_name, COUNT(*) AS row_count FROM bookings
UNION ALL
SELECT 'booking_approval_history' AS table_name, COUNT(*) AS row_count FROM booking_approval_history;

-- Expected:
-- users >= 4
-- venues >= 3
-- bookings >= 3
-- booking_approval_history >= 3

-- 2. INTEGRITY CHECKS: Ensure no orphaned bookings
SELECT b.id AS orphaned_booking_id
FROM bookings b
LEFT JOIN users u ON b.user_id = u.id
LEFT JOIN venues v ON b.venue_id = v.id
WHERE u.id IS NULL OR v.id IS NULL;

-- 3. SEQUENCES VERIFICATION
SELECT
    'users' AS table_name, last_value FROM users_id_seq
UNION ALL
SELECT
    'venues' AS table_name, last_value FROM venues_id_seq
UNION ALL
SELECT
    'bookings' AS table_name, last_value FROM bookings_id_seq
UNION ALL
SELECT
    'booking_approval_history' AS table_name, last_value FROM booking_approval_history_id_seq;
