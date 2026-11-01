package com.rishi.venue.controller;

import com.rishi.venue.dto.analytics.AdminAnalyticsData;
import com.rishi.venue.model.AuditLog;
import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;
import com.rishi.venue.service.AnalyticsService;
import com.rishi.venue.service.AuditLogService;
import com.rishi.venue.service.BookingService;
import com.rishi.venue.service.VenueService;
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
    private final AnalyticsService analyticsService;
    private final AuditLogService auditLogService;
    private final VenueService venueService;

    public AdminController(
            BookingService bookingService,
            UserRepository userRepository) {
        this(bookingService, userRepository, null, null, null, null);
    }

    public AdminController(
            BookingService bookingService,
            UserRepository userRepository,
            VenueRepository venueRepository) {
        this(bookingService, userRepository, venueRepository, null, null, null);
    }

    public AdminController(
            BookingService bookingService,
            UserRepository userRepository,
            VenueRepository venueRepository,
            AnalyticsService analyticsService) {
        this(bookingService, userRepository, venueRepository, analyticsService, null, null);
    }

    @Autowired
    public AdminController(
            BookingService bookingService,
            UserRepository userRepository,
            @Autowired(required = false) VenueRepository venueRepository,
            @Autowired(required = false) AnalyticsService analyticsService,
            @Autowired(required = false) AuditLogService auditLogService,
            @Autowired(required = false) VenueService venueService) {

        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.analyticsService = analyticsService;
        this.auditLogService = auditLogService;
        this.venueService = venueService;
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

    @GetMapping("/analytics")
    public String viewAdminAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (analyticsService != null) {
            AdminAnalyticsData analytics = analyticsService.getAdminAnalytics(startDate, endDate);
            model.addAttribute("analytics", analytics);
        }

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin-analytics";
    }

    // ==============================================================
    // AUDIT LOG DASHBOARD (PHASE 6)
    // ==============================================================

    @GetMapping("/audit")
    public String viewAuditLogs(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<AuditLog> logs = (auditLogService != null)
                ? auditLogService.getAuditLogs(user, action, startDate, endDate)
                : List.of();

        List<String> actions = (auditLogService != null)
                ? auditLogService.getDistinctActions()
                : List.of();

        List<AuditLog> recentLogs = (auditLogService != null)
                ? auditLogService.getRecentLogs()
                : List.of();

        model.addAttribute("logs", logs);
        model.addAttribute("actions", actions);
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("selectedUser", user != null ? user.trim() : "");
        model.addAttribute("selectedAction", (action != null && !action.isBlank()) ? action.toUpperCase() : "ALL");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin-audit";
    }

    // ==============================================================
    // VENUE MANAGEMENT (PHASE 6 AUDIT LOGGING)
    // ==============================================================

    @GetMapping("/venues")
    public String manageVenues(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Venue> venues = (venueService != null)
                ? venueService.getAllVenues()
                : (venueRepository != null ? venueRepository.findAll() : List.of());

        model.addAttribute("venues", venues);
        return "admin-venues";
    }

    @PostMapping("/venues/create")
    public String createVenue(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam Integer capacity,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false, defaultValue = "GENERAL") String venueType,
            @RequestParam(required = false, defaultValue = "Standard Amenities") String facilities,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            if (venueService != null) {
                venueService.createVenue(name, location, capacity, active, venueType, facilities, admin.getEmail() + " (ADMIN)");
            } else if (venueRepository != null) {
                venueRepository.save(new Venue(name, location, capacity, active, venueType, facilities));
            }
            redirectAttributes.addFlashAttribute("successMessage", "Venue '" + name + "' created successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/venues";
    }

    @PostMapping("/venues/{id}/update")
    public String updateVenue(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam Integer capacity,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false, defaultValue = "GENERAL") String venueType,
            @RequestParam(required = false, defaultValue = "Standard Amenities") String facilities,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            if (venueService != null) {
                venueService.updateVenue(id, name, location, capacity, active, venueType, facilities, admin.getEmail() + " (ADMIN)");
                redirectAttributes.addFlashAttribute("successMessage", "Venue '" + name + "' updated successfully.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/venues";
    }

    @PostMapping("/venues/{id}/toggle-status")
    public String toggleVenueStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User admin = getAdminUser(session);
        if (admin == null) {
            return "redirect:/login";
        }

        try {
            if (venueService != null) {
                Venue v = venueService.toggleActiveStatus(id, admin.getEmail() + " (ADMIN)");
                redirectAttributes.addFlashAttribute("successMessage",
                        "Venue '" + v.getName() + "' status changed to " + (v.isActive() ? "ACTIVE" : "INACTIVE") + ".");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/venues";
    }

    // ==============================================================
    // APPROVAL WORKFLOW
    // ==============================================================

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
