
package com.rishi.venue.controller;

import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class VenueController {

    private final VenueRepository venueRepository;
    private final BookingService bookingService;

    public VenueController(
            VenueRepository venueRepository,
            BookingService bookingService) {

        this.venueRepository = venueRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/venues")
    public String viewVenues(
            Model model,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("venues", venueRepository.findAll());

        return "venues";
    }

    @GetMapping("/venues/{venueId}/availability")
    public String viewAvailability(
            @PathVariable Long venueId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpSession session,
            Model model) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        Venue venue = venueRepository.findById(venueId)
                .orElse(null);

        if (venue == null) {
            return "redirect:/venues";
        }

        if (date.isBefore(LocalDate.now())) {
            model.addAttribute("error", "Please select today or a future date.");
            model.addAttribute("venue", venue);
            model.addAttribute("slots", BookingService.SLOTS);
            model.addAttribute("minDate", LocalDate.now());
            return "book";
        }

        Map<String, Boolean> availability = new LinkedHashMap<>();

        for (String slot : BookingService.SLOTS) {
            availability.put(
                    slot,
                    !bookingService.isSlotBooked(venue, date, slot)
            );
        }

        model.addAttribute("venue", venue);
        model.addAttribute("slots", BookingService.SLOTS);
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("selectedDate", date);
        model.addAttribute("availability", availability);

        return "book";
    }
}
