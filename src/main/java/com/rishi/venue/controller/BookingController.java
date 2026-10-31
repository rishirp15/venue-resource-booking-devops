package com.rishi.venue.controller;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

        Venue venue = venueRepository.findById(venueId).orElse(null);

        if (venue == null) {
            return "redirect:/venues";
        }

        model.addAttribute("venue", venue);
        model.addAttribute("slots", BookingService.SLOTS);
        model.addAttribute("minDate", LocalDate.now());
        model.addAttribute("defaultStartTime", BookingService.MORNING_START);
        model.addAttribute("defaultEndTime", BookingService.MORNING_END);

        return "book";
    }

    @PostMapping("/book")
    public String submitBooking(
            @RequestParam Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false, defaultValue = "1") Integer attendees,
            @RequestParam(required = false) String slot,
            @RequestParam(required = false) String purpose,
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
                    startTime,
                    endTime,
                    attendees,
                    slot,
                    purpose
            );

            return "redirect:/my-bookings?success";

        } catch (IllegalArgumentException ex) {

            Venue venue = venueRepository.findById(venueId).orElse(null);

            if (venue == null) {
                return "redirect:/venues";
            }

            LocalTime effectiveStart = startTime != null ? startTime : BookingService.getDefaultStartTime(slot);
            LocalTime effectiveEnd = endTime != null ? endTime : BookingService.getDefaultEndTime(slot);

            List<Venue> alternativeVenues = bookingService.findAlternativeVenues(
                    venueId,
                    bookingDate,
                    effectiveStart,
                    effectiveEnd,
                    attendees
            );

            model.addAttribute("venue", venue);
            model.addAttribute("slots", BookingService.SLOTS);
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("selectedDate", bookingDate);
            model.addAttribute("selectedSlot", slot);
            model.addAttribute("selectedStartTime", startTime);
            model.addAttribute("selectedEndTime", endTime);
            model.addAttribute("selectedAttendees", attendees);
            model.addAttribute("selectedPurpose", purpose);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("alternativeVenues", alternativeVenues);

            return "book";
        }
    }

    @GetMapping("/bookings/{bookingId}")
    public String viewBookingDetails(
            @PathVariable Long bookingId,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingDetails(bookingId, userId, userRole);
            model.addAttribute("booking", booking);
            return "booking-details";
        } catch (IllegalArgumentException | SecurityException ex) {
            return "redirect:/my-bookings";
        }
    }

    @GetMapping("/bookings/{bookingId}/reschedule")
    public String showRescheduleForm(
            @PathVariable Long bookingId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.getBookingDetails(bookingId, userId, userRole);

            if (!BookingService.ACTIVE_STATUSES.contains(booking.getStatus())) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Only pending or confirmed bookings can be rescheduled."
                );
                return "redirect:/my-bookings";
            }

            model.addAttribute("booking", booking);
            model.addAttribute("venue", booking.getVenue());
            model.addAttribute("slots", BookingService.SLOTS);
            model.addAttribute("minDate", LocalDate.now());

            return "reschedule";

        } catch (IllegalArgumentException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/my-bookings";
        }
    }

    @PostMapping("/bookings/{bookingId}/reschedule")
    public String processReschedule(
            @PathVariable Long bookingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) String slot,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        try {
            bookingService.rescheduleBooking(
                    bookingId,
                    userId,
                    bookingDate,
                    startTime,
                    endTime,
                    slot
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking rescheduled successfully! Your request has been updated and is pending confirmation."
            );

            return "redirect:/bookings/" + bookingId;

        } catch (IllegalArgumentException | SecurityException ex) {
            Venue venue = venueRepository.findAll().stream().findFirst().orElse(null);
            try {
                Booking booking = bookingService.getBookingDetails(bookingId, userId, (String) session.getAttribute("userRole"));
                venue = booking.getVenue();
                model.addAttribute("booking", booking);
            } catch (Exception ignored) {}

            LocalTime effectiveStart = startTime != null ? startTime : BookingService.getDefaultStartTime(slot);
            LocalTime effectiveEnd = endTime != null ? endTime : BookingService.getDefaultEndTime(slot);

            List<Venue> alternativeVenues = bookingService.findAlternativeVenues(
                    venue != null ? venue.getId() : null,
                    bookingDate,
                    effectiveStart,
                    effectiveEnd,
                    1
            );

            model.addAttribute("venue", venue);
            model.addAttribute("slots", BookingService.SLOTS);
            model.addAttribute("minDate", LocalDate.now());
            model.addAttribute("selectedDate", bookingDate);
            model.addAttribute("selectedSlot", slot);
            model.addAttribute("selectedStartTime", startTime);
            model.addAttribute("selectedEndTime", endTime);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("alternativeVenues", alternativeVenues);

            return "reschedule";
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
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        try {
            bookingService.cancelBooking(bookingId, userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking cancelled successfully."
            );

        } catch (IllegalArgumentException | SecurityException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/my-bookings";
    }
}
