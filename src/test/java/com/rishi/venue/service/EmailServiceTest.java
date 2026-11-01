package com.rishi.venue.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Test
    @DisplayName("Should skip sending email when disabled")
    void shouldSkipWhenEmailDisabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        boolean sent = service.sendEmail("user@example.com", "Test Subject", "Test Body");
        assertFalse(sent);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Should skip sending email when JavaMailSender is null")
    void shouldSkipWhenMailSenderNull() {
        EmailService service = new EmailService(null);
        ReflectionTestUtils.setField(service, "emailEnabled", true);

        boolean sent = service.sendEmail("user@example.com", "Test Subject", "Test Body");
        assertFalse(sent);
    }

    @Test
    @DisplayName("Should send email when enabled and sender available")
    void shouldSendEmailWhenEnabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@venue.com");

        boolean sent = service.sendEmail("user@example.com", "Test Subject", "Test Body");
        assertTrue(sent);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should gracefully handle exceptions from JavaMailSender without failing")
    void shouldGracefullyHandleMailSenderException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("SMTP Server Unreachable")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@venue.com");

        boolean sent = service.sendEmail("user@example.com", "Test Subject", "Test Body");
        assertFalse(sent); // Caught gracefully
    }
}
