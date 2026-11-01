package com.rishi.venue.dto.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdminAnalyticsData(
        long totalBookings,
        long approvedBookings,
        long pendingBookings,
        long cancelledBookings,
        long rejectedBookings,
        long completedBookings,
        List<MonthlyTrend> monthlyTrends,
        List<VenueBookingMetric> mostBookedVenues,
        List<VenueBookingMetric> leastUtilizedVenues,
        Map<String, Long> statusDistribution,
        List<DayOfWeekMetric> peakBookingDays,
        List<HourMetric> peakBookingHours,
        List<VenueUtilizationMetric> venueUtilizations,
        LocalDate startDate,
        LocalDate endDate
) {
}

