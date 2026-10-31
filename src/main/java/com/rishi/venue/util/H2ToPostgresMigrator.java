package com.rishi.venue.util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Migration Utility to transfer data from H2 database file to PostgreSQL.
 * Verifies record counts and synchronizes identity sequences.
 */
public class H2ToPostgresMigrator {

    private static final Logger log = Logger.getLogger(H2ToPostgresMigrator.class.getName());

    public static void main(String[] args) {
        String h2Url = System.getenv().getOrDefault("H2_URL", "jdbc:h2:file:./data/venuedb;IFEXISTS=TRUE");
        String h2User = System.getenv().getOrDefault("H2_USER", "sa");
        String h2Pass = System.getenv().getOrDefault("H2_PASSWORD", "");

        String pgHost = System.getenv().getOrDefault("DB_HOST", "localhost");
        String pgPort = System.getenv().getOrDefault("DB_PORT", "5432");
        String pgName = System.getenv().getOrDefault("DB_NAME", "venuedb");
        String pgUrl = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgName);
        String pgUser = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME",
                System.getenv().getOrDefault("DB_USER", "postgres"));
        String pgPass = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD",
                System.getenv().getOrDefault("DB_PASSWORD", "postgres"));

        System.out.println("=================================================");
        System.out.println("Starting H2 to PostgreSQL Migration");
        System.out.println("H2 Source: " + h2Url);
        System.out.println("PostgreSQL Target: " + pgUrl);
        System.out.println("=================================================");

        try (Connection h2Conn = DriverManager.getConnection(h2Url, h2User, h2Pass);
             Connection pgConn = DriverManager.getConnection(pgUrl, pgUser, pgPass)) {

            pgConn.setAutoCommit(false);

            int userCount = migrateUsers(h2Conn, pgConn);
            int venueCount = migrateVenues(h2Conn, pgConn);
            int bookingCount = migrateBookings(h2Conn, pgConn);
            int historyCount = migrateApprovalHistory(h2Conn, pgConn);

            syncSequences(pgConn);

            pgConn.commit();

            System.out.println("\nMigration successfully committed!");
            System.out.println("---------------------------------");
            System.out.println("Users migrated:           " + userCount);
            System.out.println("Venues migrated:          " + venueCount);
            System.out.println("Bookings migrated:        " + bookingCount);
            System.out.println("Approval history migrated: " + historyCount);
            System.out.println("---------------------------------");

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int migrateUsers(Connection h2Conn, Connection pgConn) throws SQLException {
        String select = "SELECT id, name, email, password, role FROM users";
        String upsert = """
            INSERT INTO users (id, name, email, password, role)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                email = EXCLUDED.email,
                password = EXCLUDED.password,
                role = EXCLUDED.role
            """;

        int count = 0;
        try (Statement stmt = h2Conn.createStatement();
             ResultSet rs = stmt.executeQuery(select);
             PreparedStatement ps = pgConn.prepareStatement(upsert)) {

            while (rs.next()) {
                ps.setLong(1, rs.getLong("id"));
                ps.setString(2, rs.getString("name"));
                ps.setString(3, rs.getString("email"));
                ps.setString(4, rs.getString("password"));
                ps.setString(5, rs.getString("role"));
                ps.executeUpdate();
                count++;
            }
        }
        return count;
    }

    public static int migrateVenues(Connection h2Conn, Connection pgConn) throws SQLException {
        // Handle optional columns if venues table has expanded
        DatabaseMetaData meta = h2Conn.getMetaData();
        boolean hasActive = columnExists(meta, "VENUES", "ACTIVE");
        boolean hasVenueType = columnExists(meta, "VENUES", "VENUE_TYPE");
        boolean hasFacilities = columnExists(meta, "VENUES", "FACILITIES");

        StringBuilder query = new StringBuilder("SELECT id, name, location, capacity");
        if (hasActive) query.append(", active");
        if (hasVenueType) query.append(", venue_type");
        if (hasFacilities) query.append(", facilities");
        query.append(" FROM venues");

        String upsert = """
            INSERT INTO venues (id, name, location, capacity, active, venue_type, facilities)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                location = EXCLUDED.location,
                capacity = EXCLUDED.capacity,
                active = EXCLUDED.active,
                venue_type = EXCLUDED.venue_type,
                facilities = EXCLUDED.facilities
            """;

        int count = 0;
        try (Statement stmt = h2Conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.toString());
             PreparedStatement ps = pgConn.prepareStatement(upsert)) {

            while (rs.next()) {
                ps.setLong(1, rs.getLong("id"));
                ps.setString(2, rs.getString("name"));
                ps.setString(3, rs.getString("location"));
                ps.setInt(4, rs.getInt("capacity"));
                ps.setBoolean(5, hasActive ? rs.getBoolean("active") : true);
                ps.setString(6, hasVenueType ? rs.getString("venue_type") : "GENERAL");
                ps.setString(7, hasFacilities ? rs.getString("facilities") : "Standard Amenities");
                ps.executeUpdate();
                count++;
            }
        }
        return count;
    }

    public static int migrateBookings(Connection h2Conn, Connection pgConn) throws SQLException {
        DatabaseMetaData meta = h2Conn.getMetaData();
        boolean hasStartTime = columnExists(meta, "BOOKINGS", "START_TIME");
        boolean hasEndTime = columnExists(meta, "BOOKINGS", "END_TIME");
        boolean hasAttendees = columnExists(meta, "BOOKINGS", "ATTENDEES");
        boolean hasPurpose = columnExists(meta, "BOOKINGS", "PURPOSE");
        boolean hasRejectionReason = columnExists(meta, "BOOKINGS", "REJECTION_REASON");

        StringBuilder query = new StringBuilder("SELECT id, user_id, venue_id, booking_date, slot, status");
        if (hasStartTime) query.append(", start_time");
        if (hasEndTime) query.append(", end_time");
        if (hasAttendees) query.append(", attendees");
        if (hasPurpose) query.append(", purpose");
        if (hasRejectionReason) query.append(", rejection_reason");
        query.append(" FROM bookings");

        String upsert = """
            INSERT INTO bookings (id, user_id, venue_id, booking_date, slot, start_time, end_time, attendees, purpose, status, rejection_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                rejection_reason = EXCLUDED.rejection_reason
            """;

        int count = 0;
        try (Statement stmt = h2Conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.toString());
             PreparedStatement ps = pgConn.prepareStatement(upsert)) {

            while (rs.next()) {
                ps.setLong(1, rs.getLong("id"));
                ps.setLong(2, rs.getLong("user_id"));
                ps.setLong(3, rs.getLong("venue_id"));
                ps.setDate(4, rs.getDate("booking_date"));
                String slot = rs.getString("slot");
                ps.setString(5, slot);

                Time startTime = hasStartTime ? rs.getTime("start_time") : null;
                Time endTime = hasEndTime ? rs.getTime("end_time") : null;
                if (startTime == null && slot != null) {
                    startTime = switch (slot.toUpperCase()) {
                        case "AFTERNOON" -> Time.valueOf("13:00:00");
                        case "EVENING" -> Time.valueOf("17:00:00");
                        default -> Time.valueOf("09:00:00");
                    };
                }
                if (endTime == null && slot != null) {
                    endTime = switch (slot.toUpperCase()) {
                        case "AFTERNOON" -> Time.valueOf("16:00:00");
                        case "EVENING" -> Time.valueOf("20:00:00");
                        default -> Time.valueOf("12:00:00");
                    };
                }
                ps.setTime(6, startTime);
                ps.setTime(7, endTime);
                ps.setInt(8, hasAttendees ? rs.getInt("attendees") : 1);
                ps.setString(9, hasPurpose ? rs.getString("purpose") : "General Event");

                String status = rs.getString("status");
                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    status = "APPROVED";
                }
                ps.setString(10, status);
                ps.setString(11, hasRejectionReason ? rs.getString("rejection_reason") : null);

                ps.executeUpdate();
                count++;
            }
        }
        return count;
    }

    public static int migrateApprovalHistory(Connection h2Conn, Connection pgConn) throws SQLException {
        DatabaseMetaData meta = h2Conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "PUBLIC", "BOOKING_APPROVAL_HISTORY", new String[]{"TABLE"})) {
            if (!rs.next()) {
                return 0;
            }
        }

        String select = "SELECT id, booking_id, previous_status, new_status, action_performed_by, action_timestamp, remarks FROM booking_approval_history";
        String upsert = """
            INSERT INTO booking_approval_history (id, booking_id, previous_status, new_status, action_performed_by, action_timestamp, remarks)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                booking_id = EXCLUDED.booking_id,
                previous_status = EXCLUDED.previous_status,
                new_status = EXCLUDED.new_status,
                action_performed_by = EXCLUDED.action_performed_by,
                action_timestamp = EXCLUDED.action_timestamp,
                remarks = EXCLUDED.remarks
            """;

        int count = 0;
        try (Statement stmt = h2Conn.createStatement();
             ResultSet rs = stmt.executeQuery(select);
             PreparedStatement ps = pgConn.prepareStatement(upsert)) {

            while (rs.next()) {
                ps.setLong(1, rs.getLong("id"));
                ps.setLong(2, rs.getLong("booking_id"));
                ps.setString(3, rs.getString("previous_status"));
                ps.setString(4, rs.getString("new_status"));
                ps.setString(5, rs.getString("action_performed_by"));
                ps.setTimestamp(6, rs.getTimestamp("action_timestamp"));
                ps.setString(7, rs.getString("remarks"));
                ps.executeUpdate();
                count++;
            }
        }
        return count;
    }

    private static void syncSequences(Connection pgConn) throws SQLException {
        String[] tables = {"users", "venues", "bookings", "booking_approval_history"};
        try (Statement stmt = pgConn.createStatement()) {
            for (String table : tables) {
                String sql = String.format(
                        "SELECT setval(pg_get_serial_sequence('%s', 'id'), COALESCE(MAX(id), 1)) FROM %s",
                        table, table
                );
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Sequence might not exist or be named differently if table is empty
                    log.warning("Notice on syncing sequence for table " + table + ": " + e.getMessage());
                }
            }
        }
    }

    private static boolean columnExists(DatabaseMetaData meta, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}

