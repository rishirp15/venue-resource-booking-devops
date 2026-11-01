package com.rishi.venue.dto.analytics;

public record VenueBookingMetric(
        Long venueId,
        String venueName,
        String location,
        int capacity,
        long bookingCount,
        double sharePercentage
) {
}

