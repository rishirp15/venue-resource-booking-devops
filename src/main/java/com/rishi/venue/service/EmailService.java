package com.rishi.venue.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Email Notification Service.
 * Allows sending email alerts if SMTP is configured.
 * Safely handles missing configurations without failing business transactions.
 */
@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.from:notifications@venue.local}")
    private String fromAddress;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isEmailEnabled() {
        return emailEnabled && mailSender != null;
    }

    /**
     * Send email notification safely.
     * If SMTP is not configured or fails, logs message and allows normal application flow.
     */
    public boolean sendEmail(String toEmail, String subject, String textBody) {
        if (!emailEnabled) {
            log.fine("Email notifications are disabled (app.notifications.email.enabled=false).");
            return false;
        }

        if (mailSender == null) {
            log.fine("JavaMailSender bean is not available. Skipping email delivery.");
            return false;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warning("Cannot send email: recipient address is empty.");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail.trim());
            message.setSubject(subject);
            message.setText(textBody);

            mailSender.send(message);
            log.info("Email notification successfully dispatched to: " + toEmail);
            return true;
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to deliver email notification to " + toEmail + ": " + ex.getMessage());
            return false;
        }
    }
}
