-- ===================================================================
-- Venue Resource Booking System - PostgreSQL Data Migration Script
-- Migrates existing H2 database records preserving IDs, roles, credentials,
-- venues, and bookings with sequence alignment.
-- ===================================================================

-- 1. USERS DATA MIGRATION
INSERT INTO users (id, name, email, password, role) VALUES
    (1, 'Rishi Test', 'rishi.test@example.com', 'Test123', 'USER'),
    (2, 'Rishi Phadale', 'rishirphadale1507@gmail.com', 'Vidyalankar@27', 'USER'),
    (3, 'Venue Administrator', 'admin@venue.local', 'admin123', 'ADMIN'),
    (4, 'Test2', 'test2@gmail.com', '$2a$10$HWBh1uVGUZWP2SejMmksPuvfqsjXJaT2KpD0ctEuq6fDBkLC7LW62', 'USER')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    role = EXCLUDED.role;

-- 2. VENUES DATA MIGRATION
INSERT INTO venues (id, name, location, capacity, active, venue_type, facilities) VALUES
    (1, 'Seminar Hall', 'Main Building', 120, TRUE, 'SEMINAR', 'Projector, Sound System, AC, Stage, Wi-Fi'),
    (2, 'Conference Room', 'Admin Building', 30, TRUE, 'CONFERENCE', 'Video Conferencing, Display Screen, Whiteboard, Wi-Fi'),
    (3, 'Computer Laboratory', 'Technology Building', 60, TRUE, 'LAB', 'Workstations, LAN, Projector, UPS Backup, AC')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    location = EXCLUDED.location,
    capacity = EXCLUDED.capacity,
    active = EXCLUDED.active,
    venue_type = EXCLUDED.venue_type,
    facilities = EXCLUDED.facilities;

-- 3. BOOKINGS DATA MIGRATION
INSERT INTO bookings (id, user_id, venue_id, booking_date, slot, start_time, end_time, attendees, purpose, status, rejection_reason) VALUES
    (1, 2, 1, '2026-10-15', 'AFTERNOON', '13:00:00', '16:00:00', 1, 'Department Technical Seminar', 'PENDING', NULL),
    (2, 2, 1, '2026-10-30', 'MORNING', '09:00:00', '12:00:00', 1, 'Campus Orientation', 'APPROVED', NULL),
    (3, 4, 1, '2026-11-05', 'EVENING', '17:00:00', '20:00:00', 1, 'Guest Keynote Presentation', 'PENDING', NULL)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    venue_id = EXCLUDED.venue_id,
    booking_date = EXCLUDED.booking_date,
    slot = EXCLUDED.slot,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    attendees = EXCLUDED.attendees,
    purpose = EXCLUDED.purpose,
    status = EXCLUDED.status,
    rejection_reason = EXCLUDED.rejection_reason;

-- 4. BOOKING APPROVAL HISTORY DATA MIGRATION
INSERT INTO booking_approval_history (id, booking_id, previous_status, new_status, action_performed_by, action_timestamp, remarks) VALUES
    (1, 1, NULL, 'PENDING', 'rishirphadale1507@gmail.com', '2026-10-10 10:00:00', 'Initial booking request submitted'),
    (2, 2, 'PENDING', 'APPROVED', 'admin@venue.local', '2026-10-11 11:30:00', 'Approved by administration'),
    (3, 3, NULL, 'PENDING', 'test2@gmail.com', '2026-10-12 14:15:00', 'Initial booking request submitted')
ON CONFLICT (id) DO UPDATE SET
    booking_id = EXCLUDED.booking_id,
    previous_status = EXCLUDED.previous_status,
    new_status = EXCLUDED.new_status,
    action_performed_by = EXCLUDED.action_performed_by,
    action_timestamp = EXCLUDED.action_timestamp,
    remarks = EXCLUDED.remarks;

-- 5. SYNCHRONIZE SERIAL SEQUENCES
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('venues', 'id'), COALESCE(MAX(id), 1)) FROM venues;
SELECT setval(pg_get_serial_sequence('bookings', 'id'), COALESCE(MAX(id), 1)) FROM bookings;
SELECT setval(pg_get_serial_sequence('booking_approval_history', 'id'), COALESCE(MAX(id), 1)) FROM booking_approval_history;

