package com.rishi.venue.controller;

import com.rishi.venue.dto.analytics.UserAnalyticsData;
import com.rishi.venue.dto.analytics.VenueRecommendation;
import com.rishi.venue.service.AnalyticsService;
import com.rishi.venue.service.RecommendationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RecommendationService recommendationService;

    public AnalyticsController(
            AnalyticsService analyticsService,
            RecommendationService recommendationService) {

        this.analyticsService = analyticsService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/my-analytics")
    public String viewUserAnalytics(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        UserAnalyticsData analytics = analyticsService.getUserAnalytics(userId);
        model.addAttribute("analytics", analytics);

        return "user-analytics";
    }

    @GetMapping("/recommendations")
    public String viewRecommendations(
            @RequestParam(required = false) Integer attendees,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String slot,
            @RequestParam(required = false) String venueType,
            @RequestParam(required = false) String facility,
            HttpSession session,
            Model model) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        Integer effectiveAttendees = attendees != null && attendees > 0 ? attendees : 15;
        LocalDate effectiveDate = bookingDate != null ? bookingDate : LocalDate.now().plusDays(1);
        String effectiveSlot = slot != null && !slot.isBlank() ? slot : "MORNING";

        List<VenueRecommendation> recommendations = recommendationService.recommendVenues(
                effectiveAttendees,
                effectiveDate,
                startTime,
                endTime,
                effectiveSlot,
                venueType,
                facility
        );

        model.addAttribute("recommendations", recommendations);
        model.addAttribute("attendees", effectiveAttendees);
        model.addAttribute("bookingDate", effectiveDate);
        model.addAttribute("startTime", startTime);
        model.addAttribute("endTime", endTime);
        model.addAttribute("slot", effectiveSlot);
        model.addAttribute("venueType", venueType != null ? venueType : "ALL");
        model.addAttribute("facility", facility != null ? facility : "");
        model.addAttribute("venueTypes", List.of("ALL", "CONFERENCE", "SEMINAR", "LAB", "AUDITORIUM", "MEETING"));
        model.addAttribute("minDate", LocalDate.now());

        return "recommendations";
    }
}

