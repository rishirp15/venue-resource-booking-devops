-- ===================================================================
-- Venue Resource Booking System - PostgreSQL Schema Migration DDL
-- Phase 5 & 6: PostgreSQL Database Migration, Audit Logs, Notifications
-- ===================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER'
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

-- 2. VENUES TABLE
CREATE TABLE IF NOT EXISTS venues (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    venue_type VARCHAR(50) DEFAULT 'GENERAL',
    facilities VARCHAR(500) DEFAULT 'Standard Amenities'
);

CREATE INDEX IF NOT EXISTS idx_venues_active ON venues (active);
CREATE INDEX IF NOT EXISTS idx_venues_capacity ON venues (capacity);

-- 3. BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    venue_id BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    booking_date DATE NOT NULL,
    slot VARCHAR(50) NOT NULL,
    start_time TIME,
    end_time TIME,
    attendees INTEGER DEFAULT 1,
    purpose VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_venue_date ON bookings (venue_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings (status);
CREATE INDEX IF NOT EXISTS idx_bookings_date ON bookings (booking_date);
CREATE INDEX IF NOT EXISTS idx_bookings_slot ON bookings (slot);

-- 4. BOOKING APPROVAL HISTORY TABLE
CREATE TABLE IF NOT EXISTS booking_approval_history (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    action_performed_by VARCHAR(255) NOT NULL,
    action_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_history_booking_id ON booking_approval_history (booking_id);
CREATE INDEX IF NOT EXISTS idx_history_timestamp ON booking_approval_history (action_timestamp);

-- 5. AUDIT LOGS TABLE (PHASE 6)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    performed_by VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs (timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON audit_logs (performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs (entity_type, entity_id);

-- 6. NOTIFICATIONS TABLE (PHASE 6)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    booking_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_user_created ON notifications (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_booking ON notifications (booking_id);
