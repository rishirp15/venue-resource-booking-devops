package com.rishi.venue.service;

import com.rishi.venue.model.AppNotification;
import com.rishi.venue.model.User;
import com.rishi.venue.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class.getName());

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            @Autowired(required = false) EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Dispatch an in-app notification and an optional email alert.
     */
    @Transactional
    public AppNotification notify(User user, String title, String message, String type, Long bookingId) {
        if (user == null) {
            log.warning("Cannot create notification for null user.");
            return null;
        }

        AppNotification notification = new AppNotification(user, title, message, type, bookingId);
        AppNotification saved = notificationRepository.save(notification);

        // Optionally send email
        if (emailService != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendEmail(user.getEmail(), title, message);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AppNotification> getUserNotifications(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return false;
        }
        Optional<AppNotification> optionalNotification = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (optionalNotification.isPresent()) {
            AppNotification notif = optionalNotification.get();
            notif.setRead(true);
            notificationRepository.save(notif);
            return true;
        }
        return false;
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.markAllAsReadForUser(userId);
    }
}
