package com.rishi.venue.dto.analytics;

public record VenueUtilizationMetric(
        Long venueId,
        String venueName,
        String location,
        int capacity,
        long totalBookings,
        long approvedBookings,
        double utilizationPercentage
) {
}

