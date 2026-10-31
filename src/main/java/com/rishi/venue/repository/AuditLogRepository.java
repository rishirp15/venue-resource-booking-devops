package com.rishi.venue.repository;

import com.rishi.venue.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findTop20ByOrderByTimestampDesc();

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action ASC")
    List<String> findDistinctActions();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:performedBy IS NULL OR :performedBy = '' OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR :action = 'ALL' OR a.action = :action) AND " +
           "(:startDateTime IS NULL OR a.timestamp >= :startDateTime) AND " +
           "(:endDateTime IS NULL OR a.timestamp <= :endDateTime) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findWithFilters(
            @Param("performedBy") String performedBy,
            @Param("action") String action,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
