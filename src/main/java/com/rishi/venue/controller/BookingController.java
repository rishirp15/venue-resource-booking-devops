
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

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final VenueRepository venueRepository;

    public BookingController(
            BookingService bookingService,
            VenueRepository venueRepository) {

        this.bookingService = bookingService;
        this.venueRepository = venueRepository;
    }

    @GetMapping("/book/{venueId}")
    public String showBookingForm(
            @PathVariable Long venueId,
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

        model.addAttribute("venue", venue);
        model.addAttribute("slots", BookingService.SLOTS);
        model.addAttribute("minDate", LocalDate.now());

        return "book";
    }

    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam String slot,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        try {

            bookingService.createBooking(
                    userId,
                    venueId,
                    bookingDate,
                    slot);

            return "redirect:/my-bookings?success";

        } catch (IllegalArgumentException ex) {

            Venue venue = venueRepository.findById(venueId)
                    .orElse(null);

            if (venue == null) {
                return "redirect:/venues";
            }

            model.addAttribute("venue", venue);
            model.addAttribute("slots", BookingService.SLOTS);
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("selectedDate", bookingDate);
            model.addAttribute("selectedSlot", slot);
            model.addAttribute("error", ex.getMessage());

            return "book";
        }
    }

    @GetMapping("/my-bookings")
    public String myBookings(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "bookings",
                bookingService.getUserBookings(userId));

        return "my-bookings";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long bookingId,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(bookingId, userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking cancelled successfully.");

        } catch (IllegalArgumentException | SecurityException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage());
        }

        return "redirect:/my-bookings";
    }

}
