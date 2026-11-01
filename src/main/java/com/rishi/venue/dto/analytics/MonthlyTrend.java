package com.rishi.venue.dto.analytics;

public record MonthlyTrend(
        String yearMonth,
        String monthLabel,
        long count
) {
}

