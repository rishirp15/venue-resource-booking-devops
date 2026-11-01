package com.rishi.venue.service;

import com.rishi.venue.model.AppNotification;
import com.rishi.venue.model.User;
import com.rishi.venue.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private EmailService emailService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        emailService = mock(EmailService.class);
        notificationService = new NotificationService(notificationRepository, emailService);
    }

    @Test
    @DisplayName("Should create and persist an in-app notification and trigger email")
    void shouldCreateNotificationAndTriggerEmail() {
        User user = new User("Alice", "alice@example.com", "hash", "USER");
        user.setId(5L);

        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(inv -> {
            AppNotification n = inv.getArgument(0);
            n.setId(10L);
            return n;
        });

        AppNotification created = notificationService.notify(
                user,
                "Booking Approved",
                "Your booking for Seminar Hall has been approved.",
                "BOOKING_APPROVED",
                42L
        );

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("Booking Approved", created.getTitle());
        assertEquals("BOOKING_APPROVED", created.getType());
        assertEquals(42L, created.getBookingId());
        assertFalse(created.isRead());

        verify(notificationRepository, times(1)).save(any(AppNotification.class));
        verify(emailService, times(1)).sendEmail(
                eq("alice@example.com"),
                eq("Booking Approved"),
                eq("Your booking for Seminar Hall has been approved.")
        );
    }

    @Test
    @DisplayName("Should retrieve user notifications and unread count")
    void shouldRetrieveUserNotificationsAndUnreadCount() {
        User user = new User("Bob", "bob@example.com", "hash", "USER");
        user.setId(7L);

        AppNotification notif1 = new AppNotification(user, "Alert 1", "Msg 1", "BOOKING_SUBMITTED", 1L);
        AppNotification notif2 = new AppNotification(user, "Alert 2", "Msg 2", "BOOKING_APPROVED", 2L);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(notif1, notif2));
        when(notificationRepository.countByUserIdAndReadFalse(7L)).thenReturn(2L);

        List<AppNotification> list = notificationService.getUserNotifications(7L);
        assertEquals(2, list.size());

        long unread = notificationService.getUnreadCount(7L);
        assertEquals(2, unread);
    }

    @Test
    @DisplayName("Should mark notification as read enforcing user ownership")
    void shouldMarkAsReadEnforcingUserOwnership() {
        User user = new User("Carol", "carol@example.com", "hash", "USER");
        user.setId(10L);

        AppNotification notif = new AppNotification(user, "Title", "Body", "SYSTEM", null);
        notif.setId(100L);
        notif.setRead(false);

        // Success case: Notification belongs to user 10
        when(notificationRepository.findByIdAndUserId(100L, 10L)).thenReturn(Optional.of(notif));
        boolean marked = notificationService.markAsRead(100L, 10L);
        assertTrue(marked);
        assertTrue(notif.isRead());
        verify(notificationRepository).save(notif);

        // Failure case: Another user tries to mark it as read
        when(notificationRepository.findByIdAndUserId(100L, 999L)).thenReturn(Optional.empty());
        boolean denied = notificationService.markAsRead(100L, 999L);
        assertFalse(denied);
    }

    @Test
    @DisplayName("Should mark all notifications as read for user")
    void shouldMarkAllAsReadForUser() {
        when(notificationRepository.markAllAsReadForUser(10L)).thenReturn(5);

        int updated = notificationService.markAllAsRead(10L);
        assertEquals(5, updated);
        verify(notificationRepository).markAllAsReadForUser(10L);
    }
}
