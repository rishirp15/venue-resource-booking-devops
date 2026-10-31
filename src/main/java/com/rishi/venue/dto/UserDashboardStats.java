package com.rishi.venue.dto;

import com.rishi.venue.model.Booking;

import java.util.List;

public record UserDashboardStats(
        long totalBookings,
        long pendingBookings,
        long confirmedBookings,
        long cancelledBookings,
        long upcomingBookings,
        List<Booking> upcomingBookingList,
        List<Booking> recentBookings
) {
}

