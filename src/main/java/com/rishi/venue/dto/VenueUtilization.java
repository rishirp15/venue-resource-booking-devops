package com.rishi.venue.dto;

import com.rishi.venue.model.Venue;

public record VenueUtilization(
        Venue venue,
        long totalBookings,
        long activeBookings,
        long confirmedBookings
) {
}

