package com.rishi.venue.controller;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    public AdminController(
            BookingService bookingService,
            UserRepository userRepository) {
        this(bookingService, userRepository, null);
    }

    @Autowired
    public AdminController(
            BookingService bookingService,
            UserRepository userRepository,
            @Autowired(required = false) VenueRepository venueRepository) {

        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    private User getAdminUser(HttpSession session) {
        Object sessionUserId = session.getAttribute("userId");
        if (!(sessionUserId instanceof Long userId)) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        return (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) ? user : null;
    }

    private boolean isAdmin(HttpSession session) {
        return getAdminUser(session) != null;
    }

    @GetMapping("")
    public String adminRoot(HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("stats", bookingService.getAdminDashboardStats());
        return "admin-dashboard";
    }

    @GetMapping("/bookings")
    public String viewAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long venueId,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getBookingsWithFilters(status, date, venueId);
        List<Venue> venues = venueRepository != null ? venueRepository.findAll() : List.of();
        Map<String, Long> statusCounts = bookingService.getBookingStatusCounts();

        model.addAttribute("bookings", bookings);
        model.addAttribute("venues", venues);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("selectedStatus", status != null && !status.isBlank() ? status.toUpperCase() : "ALL");
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedVenueId", venueId);

        return "admin-bookings";
    }

    @PostMapping("/bookings/{bookingId}/approve")
    public String approveBooking(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            bookingService.approveBooking(bookingId, admin.getEmail() + " (ADMIN)", remarks);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking #" + bookingId + " has been approved successfully."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/reject")
    public String rejectBooking(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "A reason is required to reject a booking."
            );
            return "redirect:/admin/bookings";
        }

        try {
            bookingService.rejectBooking(bookingId, admin.getEmail() + " (ADMIN)", rejectionReason.trim());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking #" + bookingId + " has been rejected."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirmBooking(
            @PathVariable Long bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            bookingService.confirmBooking(bookingId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking confirmed successfully."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{bookingId}/complete")
    public String completeBooking(
            @PathVariable Long bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            bookingService.completeBooking(bookingId, admin.getEmail() + " (ADMIN)");
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking #" + bookingId + " has been marked as COMPLETED."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/bookings";
    }
}
