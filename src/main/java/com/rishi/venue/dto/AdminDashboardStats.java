package com.rishi.venue.dto;

import com.rishi.venue.model.Booking;

import java.util.List;

public record AdminDashboardStats(
        long totalUsers,
        long totalVenues,
        long totalBookings,
        long pendingApprovals,
        long confirmedBookings,
        long cancelledBookings,
        List<VenueUtilization> venueUtilizations,
        List<Booking> recentBookings
) {
}

