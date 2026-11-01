package com.rishi.venue.dto.analytics;

import com.rishi.venue.model.Venue;
import java.util.List;

public record VenueRecommendation(
        Venue venue,
        int matchScore,
        double capacityUtilizationRatio,
        String capacityFitDescription,
        List<String> matchHighlights
) {
}

