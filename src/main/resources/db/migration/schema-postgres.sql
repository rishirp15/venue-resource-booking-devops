-- ===================================================================
-- Venue Resource Booking System - PostgreSQL Schema Migration DDL
-- Phase 5: PostgreSQL Database Migration
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
