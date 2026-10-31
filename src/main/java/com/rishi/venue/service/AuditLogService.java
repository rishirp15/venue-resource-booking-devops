package com.rishi.venue.service;

import com.rishi.venue.model.AuditLog;
import com.rishi.venue.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

@Service
public class AuditLogService {

    private static final Logger log = Logger.getLogger(AuditLogService.class.getName());

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an audit log event.
     * Transactional with REQUIRED propagation to save in same or new transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog log(String performedBy, String action, String entityType, Long entityId, String details) {
        if (performedBy == null || performedBy.isBlank()) {
            performedBy = "System";
        }
        if (action == null || action.isBlank()) {
            action = "UNKNOWN";
        }
        if (entityType == null || entityType.isBlank()) {
            entityType = "GENERAL";
        }

        AuditLog auditLog = new AuditLog(performedBy, action.toUpperCase(), entityType.toUpperCase(), entityId, details);
        try {
            AuditLog saved = auditLogRepository.save(auditLog);
            log.info(String.format("AUDIT: [%s] %s by %s on %s#%s - %s",
                    saved.getTimestamp(), action, performedBy, entityType, entityId, details));
            return saved;
        } catch (Exception e) {
            log.warning("Failed to persist audit log: " + e.getMessage());
            return auditLog;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(String userKeyword, String action, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        return auditLogRepository.findWithFilters(
                (userKeyword != null && !userKeyword.isBlank()) ? userKeyword.trim() : null,
                (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) ? action.trim().toUpperCase() : null,
                startDateTime,
                endDateTime
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }
}
