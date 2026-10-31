-- ===================================================================
-- Venue Resource Booking System - Rollback to H2 Script
-- In case of emergency rollback, H2 embedded database can be restored.
-- ===================================================================

-- Step 1: Switch Spring profile to 'dev' in environment or docker-compose:
--         export SPRING_PROFILES_ACTIVE=dev

-- Step 2 (Optional): Restore H2 from verified backup if corrupted:
--         Windows PowerShell:
--         Copy-Item ".\data\backup\venuedb.mv.db.bak" -Destination ".\data\venuedb.mv.db" -Force
--         Linux / Bash:
--         cp data/backup/venuedb.mv.db.bak data/venuedb.mv.db

-- Step 3: If new records from PostgreSQL need to be synced into H2 before rollback:
-- Run these statements in H2 Web Console (/h2-console):

MERGE INTO users (id, name, email, password, role) KEY(id)
VALUES (1, 'Rishi Test', 'rishi.test@example.com', 'Test123', 'USER');

MERGE INTO users (id, name, email, password, role) KEY(id)
VALUES (2, 'Rishi Phadale', 'rishirphadale1507@gmail.com', 'Vidyalankar@27', 'USER');

MERGE INTO users (id, name, email, password, role) KEY(id)
VALUES (3, 'Venue Administrator', 'admin@venue.local', 'admin123', 'ADMIN');

MERGE INTO users (id, name, email, password, role) KEY(id)
VALUES (4, 'Test2', 'test2@gmail.com', '$2a$10$HWBh1uVGUZWP2SejMmksPuvfqsjXJaT2KpD0ctEuq6fDBkLC7LW62', 'USER');
