package com.rishi.venue.repository;

import com.rishi.venue.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AppNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    Optional<AppNotification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadForUser(@Param("userId") Long userId);
}
