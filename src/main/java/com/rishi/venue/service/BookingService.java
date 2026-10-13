
package com.rishi.venue.service;

import com.rishi.venue.model.Booking;
import com.rishi.venue.model.User;
import com.rishi.venue.model.Venue;
import com.rishi.venue.repository.BookingRepository;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.repository.VenueRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    public static final Set<String> ACTIVE_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    public static final List<String> SLOTS =
            List.of("MORNING", "AFTERNOON", "EVENING");

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            VenueRepository venueRepository,
            UserRepository userRepository) {

        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }

    public boolean isSlotBooked(
            Venue venue,
            LocalDate date,
            String slot) {

        return bookingRepository
                .existsByVenueAndBookingDateAndSlotAndStatusIn(
                        venue,
                        date,
                        slot,
                        ACTIVE_STATUSES
                );
    }

    @Transactional
    public Booking createBooking(
            Long userId,
            Long venueId,
            LocalDate date,
            String slot) {

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Please select today or a future date."
            );
        }

        if (slot == null || !SLOTS.contains(slot)) {
            throw new IllegalArgumentException(
                    "Please select a valid booking slot."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found."
                        )
                );

        if (isSlotBooked(venue, date, slot)) {
            throw new IllegalArgumentException(
                    "This venue is already booked for the selected date and slot."
            );
        }

        Booking booking = new Booking(
                user,
                venue,
                date,
                slot
        );

        return bookingRepository.save(booking);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository
                .findByUserIdOrderByBookingDateDesc(userId);
    }

    
@Transactional
public Booking confirmBooking(Long bookingId) {

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Booking not found."
                    )
            );

    if (!"PENDING".equals(booking.getStatus())) {
        throw new IllegalArgumentException(
                "Only pending bookings can be confirmed."
        );
    }

    booking.setStatus("CONFIRMED");

    return bookingRepository.save(booking);
}

@Transactional
public Booking cancelBooking(Long bookingId, Long userId) {

    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Booking not found."
                    )
            );

    if (!booking.getUser().getId().equals(userId)) {
        throw new SecurityException(
                "You are not authorized to cancel this booking."
        );
    }

    if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
        throw new IllegalArgumentException(
                "This booking cannot be cancelled."
        );
    }

    booking.setStatus("CANCELLED");

    return bookingRepository.save(booking);
}

public List<Booking> getAllBookings() {
    return bookingRepository.findAllByOrderByBookingDateDesc();
}

}
