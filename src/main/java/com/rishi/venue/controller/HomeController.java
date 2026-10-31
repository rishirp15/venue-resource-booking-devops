package com.rishi.venue.controller;

import com.rishi.venue.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final BookingService bookingService;

    public HomeController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        Object sessionUserId = session.getAttribute("userId");

        if (sessionUserId instanceof Long userId) {
            String role = (String) session.getAttribute("userRole");
            if ("ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            }
            model.addAttribute("dashboard", bookingService.getUserDashboardStats(userId));
        }

        return "index";
    }
}
