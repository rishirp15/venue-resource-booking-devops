package com.rishi.venue.dto.analytics;

import com.rishi.venue.model.Booking;
import java.util.List;
import java.util.Map;

public record UserAnalyticsData(
        long totalBookings,
        long completedBookings,
        long approvedBookings,
        long pendingBookings,
        long cancelledBookings,
        long rejectedBookings,
        double completionRate,
        long upcomingBookingsCount,
        List<Booking> upcomingBookings,
        Map<String, Long> statusDistribution,
        List<VenueBookingMetric> frequentlyUsedVenues
) {
}

