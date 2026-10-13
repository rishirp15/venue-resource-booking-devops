
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

    private static final Set<String> ACTIVE_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    public static final List<String> SLOTS =
            List.of("MORNING", "AFTERNOON", "EVENING");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            VenueRepository venueRepository) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
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
            LocalDate bookingDate,
            String slot) {

        if (bookingDate == null ||
                bookingDate.isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Please select a valid future or current date."
            );
        }

        if (slot == null || !SLOTS.contains(slot)) {
            throw new IllegalArgumentException(
                    "Invalid booking slot."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        )
                );

        // Lock the venue before checking slot availability.
        Venue venue = venueRepository.findByIdForUpdate(venueId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found."
                        )
                );

        if (isSlotBooked(venue, bookingDate, slot)) {
            throw new IllegalArgumentException(
                    "This slot is already booked. Please choose another."
            );
        }

        Booking booking = new Booking(
                user,
                venue,
                bookingDate,
                slot
        );

        booking.setStatus("PENDING");

        return bookingRepository.save(booking);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository
                .findByUserIdOrderByBookingDateDesc(userId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository
                .findAllByOrderByBookingDateDesc();
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
    public Booking cancelBooking(
            Long bookingId,
            Long userId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found."
                        )
                );

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException(
                    "You cannot cancel another user's booking."
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
}
