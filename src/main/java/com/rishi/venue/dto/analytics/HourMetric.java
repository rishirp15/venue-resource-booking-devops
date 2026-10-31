package com.rishi.venue.dto.analytics;

public record HourMetric(
        int hour,
        String hourLabel,
        long count,
        double percentage
) {
}

