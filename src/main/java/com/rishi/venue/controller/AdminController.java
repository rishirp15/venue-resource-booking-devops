
package com.rishi.venue.controller;

import com.rishi.venue.model.User;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.service.BookingService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminController(
            BookingService bookingService,
            UserRepository userRepository) {

        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    private boolean isAdmin(HttpSession session) {

        Object sessionUserId = session.getAttribute("userId");

        if (!(sessionUserId instanceof Long userId)) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);

        return user != null && "ADMIN".equals(user.getRole());
    }

    @GetMapping("/bookings")
    public String viewAllBookings(
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/";
        }

        model.addAttribute(
                "bookings",
                bookingService.getAllBookings()
        );

        return "admin-bookings";
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirmBooking(
            @PathVariable Long bookingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/";
        }

        try {
            bookingService.confirmBooking(bookingId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Booking confirmed successfully."
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/bookings";
    }
}
