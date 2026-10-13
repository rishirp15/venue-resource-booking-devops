
package com.rishi.venue.repository;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
