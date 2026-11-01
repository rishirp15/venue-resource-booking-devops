package com.rishi.venue.controller;

import com.rishi.venue.model.AppNotification;
import com.rishi.venue.model.User;
import com.rishi.venue.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("GET /notifications redirects to /login when unauthenticated")
    void shouldRedirectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /notifications displays notifications when authenticated")
    void shouldDisplayNotificationsWhenAuthenticated() throws Exception {
        User user = new User("Alice", "alice@example.com", "hash", "USER");
        user.setId(5L);

        AppNotification n1 = new AppNotification(user, "Title 1", "Msg 1", "BOOKING_SUBMITTED", 10L);
        when(notificationService.getUserNotifications(5L)).thenReturn(List.of(n1));
        when(notificationService.getUnreadCount(5L)).thenReturn(1L);

        mockMvc.perform(get("/notifications")
                        .sessionAttr("userId", 5L)
                        .sessionAttr("userName", "Alice")
                        .sessionAttr("userRole", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attribute("unreadCount", 1L))
                .andExpect(model().attribute("totalCount", 1));
    }

    @Test
    @DisplayName("POST /notifications/{id}/read marks notification as read")
    void shouldMarkNotificationAsRead() throws Exception {
        when(notificationService.markAsRead(42L, 5L)).thenReturn(true);

        mockMvc.perform(post("/notifications/42/read")
                        .sessionAttr("userId", 5L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications?filter=all"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(notificationService).markAsRead(42L, 5L);
    }

    @Test
    @DisplayName("POST /notifications/read-all marks all notifications as read")
    void shouldMarkAllNotificationsAsRead() throws Exception {
        when(notificationService.markAllAsRead(5L)).thenReturn(3);

        mockMvc.perform(post("/notifications/read-all")
                        .sessionAttr("userId", 5L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(notificationService).markAllAsRead(5L);
    }

    @Test
    @DisplayName("GET /notifications/unread-count returns JSON count")
    void shouldReturnUnreadCountJson() throws Exception {
        when(notificationService.getUnreadCount(5L)).thenReturn(4L);

        mockMvc.perform(get("/notifications/unread-count")
                        .sessionAttr("userId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(4));
    }
}
