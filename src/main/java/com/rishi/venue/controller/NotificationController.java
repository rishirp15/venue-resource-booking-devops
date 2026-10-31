package com.rishi.venue.controller;

import com.rishi.venue.model.AppNotification;
import com.rishi.venue.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private Long getUserId(HttpSession session) {
        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId instanceof Long userId) {
            return userId;
        }
        return null;
    }

    @GetMapping("")
    public String viewNotifications(
            @RequestParam(required = false, defaultValue = "all") String filter,
            HttpSession session,
            Model model) {

        Long userId = getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        List<AppNotification> allNotifications = notificationService.getUserNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        List<AppNotification> displayedNotifications = "unread".equalsIgnoreCase(filter)
                ? allNotifications.stream().filter(n -> !n.isRead()).toList()
                : allNotifications;

        model.addAttribute("notifications", displayedNotifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("totalCount", allNotifications.size());
        model.addAttribute("currentFilter", filter.toLowerCase());

        return "notifications";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "all") String filter,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        boolean marked = notificationService.markAsRead(id, userId);
        if (marked) {
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Notification not found or access denied.");
        }

        return "redirect:/notifications?filter=" + filter;
    }

    @PostMapping("/read-all")
    public String markAllAsRead(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = getUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        int count = notificationService.markAllAsRead(userId);
        redirectAttributes.addFlashAttribute("successMessage", "All " + count + " notification(s) marked as read.");

        return "redirect:/notifications";
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("unread", 0L));
        }
        return ResponseEntity.ok(Map.of("unread", notificationService.getUnreadCount(userId)));
    }
}
