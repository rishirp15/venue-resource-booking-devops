package com.rishi.venue.service;

import com.rishi.venue.model.AuditLog;
import com.rishi.venue.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    @DisplayName("Should successfully log an audit activity")
    void shouldLogAuditActivity() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(100L);
            return log;
        });

        AuditLog result = auditLogService.log(
                "admin@venue.com",
                "BOOKING_APPROVED",
                "BOOKING",
                42L,
                "Approved department seminar"
        );

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("ADMIN@VENUE.COM", result.getPerformedBy().toUpperCase());
        assertEquals("BOOKING_APPROVED", result.getAction());
        assertEquals("BOOKING", result.getEntityType());
        assertEquals(42L, result.getEntityId());
        assertEquals("Approved department seminar", result.getDetails());
        assertNotNull(result.getTimestamp());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        assertEquals("BOOKING_APPROVED", captor.getValue().getAction());
    }

    @Test
    @DisplayName("Should handle default values when logging with nulls")
    void shouldHandleDefaultValuesWhenLoggingWithNulls() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.log(null, null, null, null, null);

        assertEquals("System", result.getPerformedBy());
        assertEquals("UNKNOWN", result.getAction());
        assertEquals("GENERAL", result.getEntityType());
        assertNull(result.getEntityId());
        assertNull(result.getDetails());
    }

    @Test
    @DisplayName("Should retrieve audit logs with filters")
    void shouldRetrieveAuditLogsWithFilters() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        AuditLog log1 = new AuditLog("user@venue.com", "BOOKING_CREATED", "BOOKING", 1L, "New booking");
        when(auditLogRepository.findWithFilters(eq("user@venue.com"), eq("BOOKING_CREATED"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(log1));

        List<AuditLog> results = auditLogService.getAuditLogs("user@venue.com", "BOOKING_CREATED", startDate, endDate);

        assertEquals(1, results.size());
        assertEquals("BOOKING_CREATED", results.get(0).getAction());
        verify(auditLogRepository).findWithFilters(eq("user@venue.com"), eq("BOOKING_CREATED"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should retrieve distinct actions and recent logs")
    void shouldRetrieveDistinctActionsAndRecentLogs() {
        when(auditLogRepository.findDistinctActions()).thenReturn(List.of("BOOKING_APPROVED", "BOOKING_CREATED", "USER_LOGIN"));
        when(auditLogRepository.findTop20ByOrderByTimestampDesc()).thenReturn(List.of(new AuditLog()));

        List<String> actions = auditLogService.getDistinctActions();
        assertEquals(3, actions.size());

        List<AuditLog> recent = auditLogService.getRecentLogs();
        assertEquals(1, recent.size());
    }
}
