package com.rishi.venue.dto.analytics;

public record DayOfWeekMetric(
        String dayName,
        int dayOfWeek,
        long count,
        double percentage
) {
}

