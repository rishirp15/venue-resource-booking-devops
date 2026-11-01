package com.rishi.venue.service;

import com.rishi.venue.dto.analytics.VenueRecommendation;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class RecommendationService {

    private final VenueRepository venueRepository;
    private final BookingService bookingService;

    public RecommendationService(VenueRepository venueRepository, BookingService bookingService) {
        this.venueRepository = venueRepository;
        this.bookingService = bookingService;
    }

    public List<VenueRecommendation> recommendVenues(
            Integer attendees,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            String slot,
            String requestedType,
            String requestedFacility) {

        int requiredCapacity = (attendees != null && attendees > 0) ? attendees : 1;
        LocalDate targetDate = (bookingDate != null) ? bookingDate : LocalDate.now().plusDays(1);

        LocalTime effectiveStart = startTime;
        LocalTime effectiveEnd = endTime;

        if (effectiveStart == null && slot != null) {
            effectiveStart = BookingService.getDefaultStartTime(slot);
        }
        if (effectiveEnd == null && slot != null) {
            effectiveEnd = BookingService.getDefaultEndTime(slot);
        }

        if (effectiveStart == null) {
            effectiveStart = BookingService.MORNING_START;
        }
        if (effectiveEnd == null) {
            effectiveEnd = BookingService.MORNING_END;
        }

        List<Venue> allVenues = venueRepository.findAll();
        List<VenueRecommendation> recommendations = new ArrayList<>();

        for (Venue venue : allVenues) {
            // Must be active
            if (venue.getActive() != null && !venue.getActive()) {
                continue;
            }

            // Must satisfy capacity
            if (venue.getCapacity() < requiredCapacity) {
                continue;
            }

            // Must be available on requested date & time
            if (bookingService.hasTimeConflict(venue, targetDate, effectiveStart, effectiveEnd, null)) {
                continue;
            }
            if (slot != null && !slot.isBlank() && bookingService.isSlotBooked(venue, targetDate, slot)) {
                continue;
            }

            List<String> highlights = new ArrayList<>();
            highlights.add("Available on " + targetDate + " (" + effectiveStart + " - " + effectiveEnd + ")");

            // 1. Capacity score (up to 40 pts)
            double capacityRatio = (double) requiredCapacity / venue.getCapacity();
            int capacityScore = (int) Math.round(capacityRatio * 40.0);
            capacityScore = Math.max(5, Math.min(40, capacityScore));

            String fitDescription;
            if (capacityRatio >= 0.70) {
                fitDescription = "Optimal Capacity Match (" + requiredCapacity + " / " + venue.getCapacity() + " seats - " + (int)(capacityRatio * 100) + "% occupancy)";
                highlights.add("High efficiency: Ideal room-to-attendee proportion");
            } else if (capacityRatio >= 0.40) {
                fitDescription = "Comfortable Fit (" + requiredCapacity + " / " + venue.getCapacity() + " seats with ample room)";
                highlights.add("Comfortable seating layout with extra breathing room");
            } else {
                fitDescription = "Spacious Room (" + requiredCapacity + " / " + venue.getCapacity() + " seats)";
                highlights.add("Spacious hall capable of supporting larger group expansions");
            }

            // 2. Type matching (up to 30 pts)
            int typeScore = 20;
            if (requestedType != null && !requestedType.isBlank() && !"ALL".equalsIgnoreCase(requestedType)) {
                if (venue.getVenueType().equalsIgnoreCase(requestedType)) {
                    typeScore = 30;
                    highlights.add("Exact match for requested space type: " + requestedType);
                } else {
                    typeScore = 10;
                }
            } else {
                highlights.add("Type: " + venue.getVenueType());
            }

            // 3. Facility matching (up to 30 pts)
            int facilityScore = 20;
            if (requestedFacility != null && !requestedFacility.isBlank()) {
                String reqLower = requestedFacility.toLowerCase().trim();
                String facLower = venue.getFacilities().toLowerCase();
                if (facLower.contains(reqLower)) {
                    facilityScore = 30;
                    highlights.add("Equipped with requested amenity: " + requestedFacility);
                } else {
                    facilityScore = 10;
                }
            } else {
                highlights.add("Facilities: " + venue.getFacilities());
            }

            int matchScore = capacityScore + typeScore + facilityScore;
            matchScore = Math.min(100, Math.max(40, matchScore));

            recommendations.add(new VenueRecommendation(
                    venue,
                    matchScore,
                    Math.round(capacityRatio * 100.0) / 100.0,
                    fitDescription,
                    highlights
            ));
        }

        recommendations.sort(
                Comparator.comparingInt(VenueRecommendation::matchScore).reversed()
                        .thenComparingInt(r -> r.venue().getCapacity())
        );

        return recommendations;
    }
}

