package com.rishi.venue.repository;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    boolean existsByVenueAndBookingDateAndSlotAndStatusIn(
            Venue venue,
            LocalDate bookingDate,
            String slot,
            Collection<String> statuses
    );

    List<Booking> findByVenueAndBookingDateAndStatusIn(
            Venue venue,
            LocalDate bookingDate,
            Collection<String> statuses
    );

    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    List<Booking> findAllByOrderByBookingDateDesc();

    List<Booking> findByStatusOrderByBookingDateDesc(String status);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR b.status = :status) AND " +
           "(:bookingDate IS NULL OR b.bookingDate = :bookingDate) AND " +
           "(:venueId IS NULL OR b.venue.id = :venueId) " +
           "ORDER BY b.bookingDate DESC, b.startTime ASC")
    List<Booking> findWithFilters(
            @Param("status") String status,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("venueId") Long venueId
    );
}
